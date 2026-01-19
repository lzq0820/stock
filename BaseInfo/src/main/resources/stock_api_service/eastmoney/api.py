#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
东方财富A股实时行情API模块
"""
import logging
import time
import json
import os
from datetime import datetime
from typing import Optional, List, Dict, Any

import akshare as ak
import pandas as pd
from fastapi import APIRouter, HTTPException, Query

# 创建路由实例（由主应用注册）
router = APIRouter()

# 为东方财富创建专门的日志记录器
eastmoney_logger = logging.getLogger("stock_api_service.eastmoney")
eastmoney_logger.setLevel(logging.INFO)

# 确保日志目录存在
log_dir = "log"
os.makedirs(log_dir, exist_ok=True)

# 创建文件处理器，记录原始数据
eastmoney_file_handler = logging.FileHandler(os.path.join(log_dir, "eastmoney_api.log"), mode='a', encoding='utf-8')
eastmoney_file_handler.setLevel(logging.INFO)
eastmoney_file_formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
eastmoney_file_handler.setFormatter(eastmoney_file_formatter)
eastmoney_logger.addHandler(eastmoney_file_handler)

def parse_stock_data(row: pd.Series) -> Dict[str, Any]:
    """解析单只股票数据"""
    stock_data = {
        # 基础信息
        "symbol": row.get("代码", ""),
        "name": row.get("名称", ""),
        "current_price": float(row.get("最新价")) if pd.notna(row.get("最新价")) else 0.0,

        # 涨跌幅相关
        "change_percent": float(row.get("涨跌幅")) if pd.notna(row.get("涨跌幅")) else 0.0,
        "change_amount": float(row.get("涨跌额")) if pd.notna(row.get("涨跌额")) else 0.0,

        # 成交数据
        "volume": int(row.get("成交量")) if pd.notna(row.get("成交量")) else 0,
        "turnover": float(row.get("成交额")) if pd.notna(row.get("成交额")) else 0.0,
        "amplitude": float(row.get("振幅")) if pd.notna(row.get("振幅")) else 0.0,

        # 价格数据
        "high_price": float(row.get("最高")) if pd.notna(row.get("最高")) else 0.0,
        "low_price": float(row.get("最低")) if pd.notna(row.get("最低")) else 0.0,
        "open_price": float(row.get("今开")) if pd.notna(row.get("今开")) else 0.0,
        "prev_close": float(row.get("昨收")) if pd.notna(row.get("昨收")) else 0.0,

        # 其他指标
        "volume_ratio": float(row.get("量比")) if pd.notna(row.get("量比")) else 0.0,
        "turnover_rate": float(row.get("换手率")) if pd.notna(row.get("换手率")) else 0.0,
        "pe_dynamic": float(row.get("市盈率-动态")) if pd.notna(row.get("市盈率-动态")) else 0.0,
        "pb_ratio": float(row.get("市净率")) if pd.notna(row.get("市净率")) else 0.0,
        "total_market_value": float(row.get("总市值")) if pd.notna(row.get("总市值")) else 0.0,
        "circulating_market_value": float(row.get("流通市值")) if pd.notna(row.get("流通市值")) else 0.0,
        "price_speed": float(row.get("涨速")) if pd.notna(row.get("涨速")) else 0.0,
        "five_minute_change": float(row.get("5分钟涨跌")) if pd.notna(row.get("5分钟涨跌")) else 0.0,
        "sixty_day_change": float(row.get("60日涨跌幅")) if pd.notna(row.get("60日涨跌幅")) else 0.0,
        "year_to_date_change": float(row.get("年初至今涨跌幅")) if pd.notna(row.get("年初至今涨跌幅")) else 0.0,

        # 内部序号
        "serial_number": int(row.get("序号")) if pd.notna(row.get("序号")) else 0
    }

    # 处理可能的NaN值
    for key, value in stock_data.items():
        if pd.isna(value):
            if isinstance(value, (int, float)):
                stock_data[key] = 0
            else:
                stock_data[key] = ""

    return stock_data

def fetch_a_stock_realtime() -> List[Dict[str, Any]]:
    """获取沪深京A股实时行情数据"""
    start_time = time.time()  # 记录开始时间
    try:
        # 调用akshare接口获取数据
        df = ak.stock_zh_a_spot_em()

        # 记录原始数据到日志
        raw_data_json = df.to_json(orient='records', force_ascii=False)
        eastmoney_logger.info(f"东方财富原始API响应数据: {raw_data_json}")

        eastmoney_logger.info(f"获取A股实时行情数据，共{len(df)}条记录，耗时: {time.time() - start_time:.2f}s")

        # 解析数据
        parsed_data = []
        for _, row in df.iterrows():
            parsed_data.append(parse_stock_data(row))

        eastmoney_logger.info(f"成功解析A股实时行情数据，共{len(parsed_data)}只股票，总耗时: {time.time() - start_time:.2f}s")
        return parsed_data

    except Exception as e:
        eastmoney_logger.error(f"获取A股实时行情数据失败", exc_info=True)
        raise HTTPException(status_code=500, detail=f"获取数据失败：{str(e)}")

def get_a_stock_statistics(stock_data: List[Dict[str, Any]]) -> Dict[str, Any]:
    """统计A股行情数据的核心指标"""
    if not stock_data:
        return {
            "code": 200,
            "msg": "success",
            "statistics": {
                "total_count": 0,
                "avg_price": 0.0,
                "avg_change_percent": 0.0,
                "avg_turnover_rate": 0.0,
                "avg_pe": 0.0,
                "avg_pb": 0.0,
                "total_market_value": 0.0,
                "circulating_market_value": 0.0,
                "up_count": 0,
                "down_count": 0,
                "limit_up_count": 0,
                "limit_down_count": 0,
                "top_stocks_by_price": [],
                "top_stocks_by_change": [],
                "top_stocks_by_volume": []
            }
        }

    # 统计指标
    total_count = len(stock_data)
    total_price = 0.0
    total_change = 0.0
    total_turnover_rate = 0.0
    total_pe = 0.0
    total_pb = 0.0
    total_mv = 0.0
    total_cmv = 0.0

    up_count = 0
    down_count = 0
    limit_up_count = 0
    limit_down_count = 0

    # 用于排序的数据
    price_list = []
    change_list = []
    volume_list = []

    for stock in stock_data:
        # 数值统计
        price = stock["current_price"]
        change = stock["change_percent"]
        turnover_rate = stock["turnover_rate"]
        pe = stock["pe_dynamic"]
        pb = stock["pb_ratio"]
        mv = stock["total_market_value"]
        cmv = stock["circulating_market_value"]

        total_price += price
        total_change += change
        total_turnover_rate += turnover_rate
        if pe > 0:
            total_pe += pe
        if pb > 0:
            total_pb += pb
        total_mv += mv
        total_cmv += cmv

        # 涨跌统计
        if change > 0:
            up_count += 1
        elif change < 0:
            down_count += 1

        # 涨跌停统计（估算）
        if change >= 9.8:
            limit_up_count += 1
        elif change <= -9.8:
            limit_down_count += 1

        # 排序用数据
        price_list.append({
            "symbol": stock["symbol"],
            "name": stock["name"],
            "price": price
        })
        change_list.append({
            "symbol": stock["symbol"],
            "name": stock["name"],
            "change_percent": change
        })
        volume_list.append({
            "symbol": stock["symbol"],
            "name": stock["name"],
            "volume": stock["volume"],
            "turnover": stock["turnover"]
        })

    # 计算平均值
    avg_price = round(total_price / total_count, 2) if total_count > 0 else 0.0
    avg_change = round(total_change / total_count, 2) if total_count > 0 else 0.0
    avg_turnover_rate = round(total_turnover_rate / total_count, 2) if total_count > 0 else 0.0
    avg_pe = round(total_pe / total_count, 2) if total_count > 0 else 0.0
    avg_pb = round(total_pb / total_count, 2) if total_count > 0 else 0.0

    # 按不同指标排序
    price_list.sort(key=lambda x: x["price"], reverse=True)
    change_list.sort(key=lambda x: x["change_percent"], reverse=True)
    volume_list.sort(key=lambda x: x["volume"], reverse=True)

    # 构建统计结果
    statistics = {
        "total_count": total_count,
        "avg_price": avg_price,
        "avg_change_percent": avg_change,
        "avg_turnover_rate": avg_turnover_rate,
        "avg_pe": avg_pe,
        "avg_pb": avg_pb,
        "total_market_value": round(total_mv, 2),
        "circulating_market_value": round(total_cmv, 2),
        "up_count": up_count,
        "down_count": down_count,
        "limit_up_count": limit_up_count,
        "limit_down_count": limit_down_count,
        "top_10_by_price": price_list[:10],
        "top_10_by_change": change_list[:10],
        "top_10_by_volume": volume_list[:10]
    }

    return {
        "code": 200,
        "msg": "success",
        "statistics": statistics,
        "detail_data": stock_data
    }

# ========== 东方财富API接口 ==========
@router.get("/api/stock/a/realtime", summary="获取沪深京A股实时行情数据")
def get_a_stock_realtime_api():
    """获取沪深京A股实时行情数据"""
    try:
        data = fetch_a_stock_realtime()
        return {
            "code": 200,
            "msg": "success",
            "trade_date": datetime.now().strftime("%Y-%m-%d"),
            "data": data,
            "count": len(data)
        }
    except HTTPException as e:
        raise e
    except Exception as e:
        eastmoney_logger.error(f"东方财富API接口异常", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": datetime.now().strftime("%Y-%m-%d"),
            "data": [],
            "count": 0
        }

@router.get("/api/stock/a/realtime/statistics", summary="统计沪深京A股实时行情数据")
def get_a_stock_statistics_api():
    """统计沪深京A股实时行情数据的核心指标"""
    try:
        raw_data = fetch_a_stock_realtime()
        result = get_a_stock_statistics(raw_data)
        result["trade_date"] = datetime.now().strftime("%Y-%m-%d")
        return result
    except HTTPException as e:
        raise e
    except Exception as e:
        eastmoney_logger.error(f"东方财富统计接口异常", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": datetime.now().strftime("%Y-%m-%d"),
            "statistics": {},
            "detail_data": []
        }

@router.get("/api/stock/a/realtime/search", summary="搜索特定股票")
def search_stock(
    keyword: str = Query(..., description="股票代码或名称关键词"),
    limit: Optional[int] = Query(20, description="返回结果数量限制，默认20")
):
    """根据关键词搜索股票"""
    try:
        all_data = fetch_a_stock_realtime()
        filtered_data = []

        for stock in all_data:
            if keyword.lower() in stock["symbol"].lower() or keyword.lower() in stock["name"].lower():
                filtered_data.append(stock)
                if len(filtered_data) >= limit:
                    break

        return {
            "code": 200,
            "msg": "success",
            "trade_date": datetime.now().strftime("%Y-%m-%d"),
            "data": filtered_data,
            "count": len(filtered_data)
        }
    except Exception as e:
        eastmoney_logger.error(f"东方财富搜索接口异常", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": datetime.now().strftime("%Y-%m-%d"),
            "data": [],
            "count": 0
        }
