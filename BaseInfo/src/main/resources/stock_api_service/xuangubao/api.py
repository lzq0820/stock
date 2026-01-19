#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
选股宝涨跌停数据API模块
"""
import logging
import json
import os
from datetime import datetime
from typing import Optional, List, Dict, Any, Union
from decimal import Decimal

import requests
import urllib3
from fastapi import APIRouter, HTTPException, Query
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# 创建路由实例（由主应用注册）
router = APIRouter()

# 为选股宝创建专门的日志记录器
xuangubao_logger = logging.getLogger("stock_api_service.xuangubao")
xuangubao_logger.setLevel(logging.INFO)

# 确保日志目录存在
log_dir = "log"
os.makedirs(log_dir, exist_ok=True)

# 创建文件处理器，记录原始数据
xuangubao_file_handler = logging.FileHandler(os.path.join(log_dir, "xuangubao_api.log"), mode='a', encoding='utf-8')
xuangubao_file_handler.setLevel(logging.INFO)
xuangubao_file_formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
xuangubao_file_handler.setFormatter(xuangubao_file_formatter)
xuangubao_logger.addHandler(xuangubao_file_handler)

# ========== 配置常量 ==========
XUANGUBAO_API_BASE = "https://flash-api.xuangubao.com.cn/api/pool/detail"
POOL_TYPES = {
    "zt": "limit_up",          # 涨停池
    "dt": "limit_down",        # 跌停池
    "yesterday_zt": "yesterday_limit_up",  # 昨日涨停
    "broken_zt": "limit_up_broken",        # 炸板池
    "super_stock": "super_stock"           # 强势股池
}

# 禁用代理，确保接口访问正常
proxy_env_vars = ['http_proxy', 'https_proxy', 'HTTP_PROXY', 'HTTPS_PROXY', 'all_proxy', 'ALL_PROXY']
for var in proxy_env_vars:
    os.environ.pop(var, None)

# 禁用urllib3警告
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def init_session() -> requests.Session:
    """初始化请求会话，配置重试和超时"""
    session = requests.Session()
    session.verify = False
    session.trust_env = False  # 强制不使用代理

    # 配置重试策略
    retry_strategy = Retry(
        total=3,
        backoff_factor=0.5,
        allowed_methods=["GET"],
        status_forcelist=[429, 500, 502, 503, 504]
    )
    adapter = HTTPAdapter(max_retries=retry_strategy)
    session.mount("https://", adapter)
    session.mount("http://", adapter)

    # 超时设置
    session.timeout = 15

    # 请求头伪装
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer": "https://xuangubao.com.cn",
        "Accept": "application/json, text/plain, */*",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8"
    })
    return session

session = init_session()

def timestamp_to_datetime(timestamp: Union[int, float, None]) -> Optional[str]:
    """时间戳转换为字符串（格式：yyyy-MM-dd HH:mm:ss）"""
    if not timestamp or timestamp == 0:
        return None
    try:
        return datetime.fromtimestamp(int(timestamp)).strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        xuangubao_logger.error(f"时间戳转换失败：{timestamp}", exc_info=True)
        return None

def calculate_lock_amount(lock_ratio: Union[float, None], market_cap: Union[float, None]) -> Optional[float]:
    """计算封单金额（封单比 * 流通市值）"""
    if lock_ratio is None or market_cap is None:
        return None
    try:
        return round(lock_ratio * market_cap, 2)
    except Exception as e:
        xuangubao_logger.error(f"封单金额计算失败：lock_ratio={lock_ratio}, market_cap={market_cap}", exc_info=True)
        return None

def parse_stock_data(stock: Dict[str, Any], pool_type: str, trade_date: str) -> Dict[str, Any]:
    """解析单只股票数据，处理空值和None"""
    # 基础数据
    base_data = {
        "trade_date": trade_date,
        "stock_code": stock.get("symbol", ""),
        "stock_name": stock.get("stock_chi_name", ""),
        "stock_type": stock.get("stock_type", 0),
        "price": round(stock.get("price", 0), 2) if stock.get("price") is not None else 0,
        "change_percent": round(stock.get("change_percent", 0) * 100, 2) if stock.get("change_percent") is not None else 0,
        "turnover_ratio": round(stock.get("turnover_ratio", 0) * 100, 2) if stock.get("turnover_ratio") is not None else 0,
        "circulation_market_cap": round(stock.get("non_restricted_capital", 0) / 100000000, 2) if stock.get("non_restricted_capital") is not None else 0,
        "total_market_cap": round(stock.get("total_capital", 0) / 100000000, 2) if stock.get("total_capital") is not None else 0,
        "issue_price": round(stock.get("issue_price", 0), 2) if stock.get("issue_price") is not None else 0,
        "listed_date": timestamp_to_datetime(stock.get("listed_date")),
        "pool_type": pool_type,
    }

    # 封单相关数据
    if pool_type in ["zt", "yesterday_zt", "broken_zt", "super_stock"]:
        buy_lock_ratio = stock.get("buy_lock_volume_ratio", 0) or 0
        current_lock_amount = calculate_lock_amount(buy_lock_ratio, stock.get("non_restricted_capital"))
        base_data.update({
            "buy_lock_ratio": round(buy_lock_ratio * 100, 4),
            "sell_lock_ratio": round(stock.get("sell_lock_volume_ratio", 0) * 100, 4) if stock.get("sell_lock_volume_ratio") is not None else 0,
            "current_lock_amount": current_lock_amount,
            "max_lock_amount": current_lock_amount,
        })
    elif pool_type == "dt":
        sell_lock_ratio = stock.get("sell_lock_volume_ratio", 0) or 0
        current_lock_amount = calculate_lock_amount(sell_lock_ratio, stock.get("non_restricted_capital"))
        base_data.update({
            "buy_lock_ratio": round(stock.get("buy_lock_volume_ratio", 0) * 100, 4) if stock.get("buy_lock_volume_ratio") is not None else 0,
            "sell_lock_ratio": round(sell_lock_ratio * 100, 4),
            "current_lock_amount": current_lock_amount,
            "max_lock_amount": current_lock_amount,
        })

    # 涨停/跌停相关数据
    if pool_type == "zt":
        base_data.update({
            "limit_up_days": stock.get("limit_up_days", 0),
            "break_limit_up_times": stock.get("break_limit_up_times", 0),
            "first_limit_up_time": timestamp_to_datetime(stock.get("first_limit_up")),
            "last_limit_up_time": timestamp_to_datetime(stock.get("last_limit_up")),
        })
    elif pool_type == "dt":
        base_data.update({
            "limit_down_days": stock.get("limit_down_days", 0),
            "break_limit_down_times": stock.get("break_limit_down_times", 0),
            "first_limit_down_time": timestamp_to_datetime(stock.get("first_limit_down")),
            "last_limit_down_time": timestamp_to_datetime(stock.get("last_limit_down")),
        })
    elif pool_type == "yesterday_zt":
        base_data.update({
            "yesterday_limit_up_days": stock.get("yesterday_limit_up_days", 0),
            "yesterday_break_limit_up_times": stock.get("yesterday_break_limit_up_times", 0),
            "yesterday_first_limit_up_time": timestamp_to_datetime(stock.get("yesterday_first_limit_up")),
            "yesterday_last_limit_up_time": timestamp_to_datetime(stock.get("yesterday_last_limit_up")),
        })
    elif pool_type == "broken_zt":
        base_data.update({
            "break_limit_up_times": stock.get("break_limit_up_times", 0),
            "first_limit_up_time": timestamp_to_datetime(stock.get("first_limit_up")),
            "last_break_limit_up_time": timestamp_to_datetime(stock.get("last_break_limit_up")),
        })
    elif pool_type == "super_stock":
        base_data.update({
            "limit_up_days": stock.get("limit_up_days", 0),
            "m_days_n_boards": f"{stock.get('m_days_n_boards_days', 0)}天{stock.get('m_days_n_boards_boards', 0)}板",
            "first_limit_up_time": timestamp_to_datetime(stock.get("first_limit_up")),
            "last_limit_up_time": timestamp_to_datetime(stock.get("last_limit_up")),
        })

    # 上涨/下跌原因
    surge_reason = stock.get("surge_reason", {}) or {}
    base_data.update({
        "stock_reason": surge_reason.get("stock_reason", ""),
        "related_plates": json.dumps([
            {"plate_name": plate.get("plate_name", ""), "plate_reason": plate.get("plate_reason", "")}
            for plate in surge_reason.get("related_plates", []) if plate
        ], ensure_ascii=False)
    })

    # 处理所有None值
    for key, value in base_data.items():
        if value is None:
            if isinstance(base_data[key], (int, float)):
                base_data[key] = 0
            else:
                base_data[key] = ""

    return base_data

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((requests.exceptions.RequestException, TimeoutError)),
    reraise=True
)
def fetch_stock_pool(pool_key: str, trade_date: Optional[str] = None) -> List[Dict[str, Any]]:
    """获取指定股票池数据"""
    if pool_key not in POOL_TYPES:
        raise HTTPException(status_code=400, detail=f"不支持的股票池类型：{pool_key}，支持类型：{list(POOL_TYPES.keys())}")

    # 处理日期参数
    if not trade_date:
        trade_date = datetime.now().strftime("%Y-%m-%d")
    else:
        try:
            datetime.strptime(trade_date, "%Y-%m-%d")
        except ValueError:
            raise HTTPException(status_code=400, detail=f"日期格式错误：{trade_date}，正确格式：yyyy-MM-dd")

    pool_name = POOL_TYPES[pool_key]

    # 调用选股宝API
    try:
        params = {"pool_name": pool_name}
        if trade_date != datetime.now().strftime("%Y-%m-%d"):
            params["date"] = trade_date

        xuangubao_logger.info(f"选股宝API请求参数: {params}")

        response = session.get(XUANGUBAO_API_BASE, params=params)
        response.raise_for_status()

        # 记录原始响应数据到日志
        raw_response = response.text
        xuangubao_logger.info(f"选股宝原始API响应: {raw_response}")

        result = response.json()

        if result.get("code") != 20000:
            xuangubao_logger.error(f"选股宝API返回错误：{result.get('message')}")
            raise HTTPException(status_code=500, detail=f"选股宝API错误：{result.get('message')}")

        raw_data = result.get("data", []) or []
        if not raw_data:
            xuangubao_logger.info(f"{trade_date}的{pool_key}股票池无数据")
            return []

        # 解析数据
        parsed_data = [parse_stock_data(stock, pool_key, trade_date) for stock in raw_data]
        xuangubao_logger.info(f"成功获取{trade_date}的{pool_key}股票池数据，共{len(parsed_data)}只股票")
        return parsed_data

    except Exception as e:
        xuangubao_logger.error(f"获取{trade_date}的{pool_key}股票池数据失败", exc_info=True)
        raise HTTPException(status_code=500, detail=f"获取数据失败：{str(e)}")

def get_stock_pool_statistics(pool_key: str, trade_date: Optional[str] = None) -> Dict[str, Any]:
    """统计指定日期、指定股票池的核心数据"""
    stock_data = fetch_stock_pool(pool_key, trade_date)
    if not stock_data:
        return {
            "code": 200,
            "msg": "success",
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "pool_key": pool_key,
            "statistics": {
                "total_count": 0,
                "avg_price": 0.0,
                "avg_change_percent": 0.0,
                "avg_turnover_ratio": 0.0,
                "total_circulation_market_cap": 0.0,
                "top_stocks_by_price": [],
                "top_stocks_by_change": []
            },
            "detail_data": []
        }

    # 计算统计指标
    total_count = len(stock_data)
    total_price = 0.0
    total_change = 0.0
    total_turnover = 0.0
    total_circulation = 0.0

    price_list = []
    change_list = []

    for stock in stock_data:
        price = float(stock.get("price", 0))
        change = float(stock.get("change_percent", 0))
        turnover = float(stock.get("turnover_ratio", 0))
        circulation = float(stock.get("circulation_market_cap", 0))

        total_price += price
        total_change += change
        total_turnover += turnover
        total_circulation += circulation

        price_list.append({
            "stock_code": stock.get("stock_code"),
            "stock_name": stock.get("stock_name"),
            "price": price
        })
        change_list.append({
            "stock_code": stock.get("stock_code"),
            "stock_name": stock.get("stock_name"),
            "change_percent": change
        })

    # 计算平均值
    avg_price = round(total_price / total_count, 2) if total_count > 0 else 0.0
    avg_change = round(total_change / total_count, 2) if total_count > 0 else 0.0
    avg_turnover = round(total_turnover / total_count, 2) if total_count > 0 else 0.0

    # 排序
    price_list.sort(key=lambda x: x["price"], reverse=True)
    change_list.sort(key=lambda x: x["change_percent"], reverse=True)

    # 构建统计结果
    statistics = {
        "total_count": total_count,
        "avg_price": avg_price,
        "avg_change_percent": avg_change,
        "avg_turnover_ratio": avg_turnover,
        "total_circulation_market_cap": round(total_circulation, 2),
        "top_10_by_price": price_list[:10],
        "top_10_by_change": change_list[:10]
    }

    return {
        "code": 200,
        "msg": "success",
        "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
        "pool_key": pool_key,
        "statistics": statistics,
        "detail_data": stock_data
    }

# ========== 选股宝API接口 ==========
@router.get("/stock/pool/{pool_key}", summary="获取指定股票池数据")
def get_stock_pool_api(
    pool_key: str,
    trade_date: Optional[str] = Query(None, description="交易日期（格式：yyyy-MM-dd），为空则默认当天")
):
    """获取股票池数据"""
    try:
        data = fetch_stock_pool(pool_key, trade_date)
        return {
            "code": 200,
            "msg": "success",
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "data": data,
            "count": len(data)
        }
    except HTTPException as e:
        raise e
    except Exception as e:
        xuangubao_logger.error(f"选股宝API接口异常", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "data": [],
            "count": 0
        }

@router.get("/stock/pool/{pool_key}/statistics", summary="统计指定股票池数据")
def get_stock_pool_statistics_api(
    pool_key: str,
    trade_date: Optional[str] = Query(None, description="交易日期（格式：yyyy-MM-dd），为空则默认当天")
):
    """统计指定日期、指定股票池的核心数据"""
    try:
        return get_stock_pool_statistics(pool_key, trade_date)
    except HTTPException as e:
        raise e
    except Exception as e:
        xuangubao_logger.error(f"选股宝统计接口异常", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "pool_key": pool_key,
            "statistics": {},
            "detail_data": []
        }

@router.get("/stock/pool/all", summary="获取所有股票池数据")
def get_all_stock_pools_api(
    trade_date: Optional[str] = Query(None, description="交易日期（格式：yyyy-MM-dd），为空则默认当天")
):
    """获取所有股票池（涨停、跌停、昨日涨停、炸板池、强势股池）数据"""
    try:
        result = {}
        total_count = 0
        for pool_key in POOL_TYPES.keys():
            data = fetch_stock_pool(pool_key, trade_date)
            result[pool_key] = data
            total_count += len(data)

        return {
            "code": 200,
            "msg": "success",
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "data": result,
            "total_count": total_count
        }
    except Exception as e:
        xuangubao_logger.error(f"获取所有股票池数据失败", exc_info=True)
        return {
            "code": 500,
            "msg": str(e),
            "trade_date": trade_date or datetime.now().strftime("%Y-%m-%d"),
            "data": {},
            "total_count": 0
        }
