#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
A股行情API服务主启动类
聚合东方财富和选股宝所有API，统一端口8000提供服务
地址：http://localhost:8000
文档：http://localhost:8000/docs
"""
import logging
import sys
import os
import json

# 添加当前目录到Python路径，确保能导入子模块
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# 导入各厂商的API路由
from eastmoney.api import router as eastmoney_router
from xuangubao.api import router as xuangubao_router

# ========== 配置日志编码，解决中文乱码 ==========
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

# 配置日志
try:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[
            logging.StreamHandler(),
            logging.FileHandler(
                "stock_api_service.log",
                mode='a',
                encoding='utf-8'
            )
        ]
    )
except PermissionError:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[logging.StreamHandler()]
    )

logger = logging.getLogger("stock_api_service")

# FastAPI主应用（单实例，统一端口）
app = FastAPI(title="A股行情综合API服务", version="1.0")

# 配置跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

# 注册各厂商的API路由（添加前缀区分）
app.include_router(eastmoney_router, prefix="/api/eastmoney", tags=["东方财富API"])
app.include_router(xuangubao_router, prefix="/api/xuangubao", tags=["选股宝API"])

# API配置验证端点
@app.get("/api/config", summary="API配置验证", tags=["配置"])
def get_api_config():
    """
    获取当前可用的API配置
    """
    try:
        config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'api_config.json')
        if os.path.exists(config_path):
            with open(config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            return {
                "code": 200,
                "msg": "success",
                "data": config,
                "count": len(config.get("apis", []))
            }
        else:
            return {
                "code": 404,
                "msg": "API配置文件不存在",
                "data": {},
                "count": 0
            }
    except Exception as e:
        logger.error(f"获取API配置失败: {e}")
        return {
            "code": 500,
            "msg": str(e),
            "data": {},
            "count": 0
        }

# 根路径健康检查
@app.get("/", summary="健康检查", tags=["基础功能"])
def health_check():
    return {
        "code": 200,
        "msg": "A股行情API服务运行正常",
        "service": "stock_api_service",
        "version": "1.0",
        "docs_url": "http://localhost:8000/docs",
        "api_count": 10  # 示例API数量
    }

# 启动服务
if __name__ == "__main__":
    # 打印依赖安装提示
    required_packages = [
        "akshare>=1.10.0",
        "pandas>=1.5.0",
        "fastapi>=0.104.1",
        "uvicorn>=0.24.0",
        "requests>=2.31.0",
        "tenacity>=8.2.3",
        "urllib3>=2.0.7"
    ]
    logger.info("=" * 60)
    logger.info("依赖安装命令:")
    logger.info(f"pip install {' '.join(required_packages)}")
    logger.info("=" * 60)

    # 启动服务（绑定所有IP，端口8000）
    uvicorn.run(
        "main:app",  # 指向当前文件的app实例
        host="0.0.0.0",
        port=8000,
        log_level="info",
        access_log=True,
        reload=False  # 生产环境关闭热重载
    )
