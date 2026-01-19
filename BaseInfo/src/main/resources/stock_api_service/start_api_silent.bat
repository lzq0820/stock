@echo off
:: A股行情API服务静默启动脚本（无窗口、无交互）
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1

:: ==================== 核心配置 ====================
set "SCRIPT_DIR=%~dp0"
set "PYTHON_EXE=D:\app\develop\python3\pythonw.exe"
set "PYTHON_BACKUP=D:\app\develop\python3\python.exe"
set "SCRIPT_PATH=!SCRIPT_DIR!main.py"
set "LOG_FILE=!SCRIPT_DIR!stock_api_service.log"
set "PID_FILE=!SCRIPT_DIR!stock_api_service.pid"
set "INSTALL_LOG=!SCRIPT_DIR!pip_install.log"

:: 依赖列表
set "REQUIRED_PACKAGES=akshare>=1.10.0 pandas>=1.5.0 fastapi>=0.104.1 uvicorn>=0.24.0 requests>=2.31.0 tenacity>=8.2.3 urllib3>=2.0.7"
:: ==================================================

:: 清空旧日志（可选）
echo [%date% %time%] === 启动服务开始 === > "!LOG_FILE!" 2>&1

:: 第一步：静默检查并安装依赖（无交互，自动安装缺失项）
echo [%date% %time%] [INFO] 开始检查Python依赖... >> "!LOG_FILE!" 2>&1
set "MISSING_PACKAGES="
for %%p in (!REQUIRED_PACKAGES!) do (
    for /f "delims=>= " %%n in ("%%p") do set "PKG_NAME=%%n"
    "!PYTHON_BACKUP!" -c "import %%n" >nul 2>&1
    if !errorlevel! neq 0 (
        set "MISSING_PACKAGES=!MISSING_PACKAGES! %%p"
    )
)

if not "!MISSING_PACKAGES!"=="" (
    echo [%date% %time%] [WARNING] 检测到缺失依赖：!MISSING_PACKAGES! >> "!LOG_FILE!" 2>&1
    echo [%date% %time%] [INFO] 自动安装缺失依赖（日志：!INSTALL_LOG!） >> "!LOG_FILE!" 2>&1
    :: 静默安装依赖，输出到安装日志
    "!PYTHON_BACKUP!" -m pip install !REQUIRED_PACKAGES! -i https://pypi.tuna.tsinghua.edu.cn/simple > "!INSTALL_LOG!" 2>&1
    if !errorlevel! neq 0 (
        echo [%date% %time%] [ERROR] 依赖安装失败，查看：!INSTALL_LOG! >> "!LOG_FILE!" 2>&1
        exit /b 1
    )
    echo [%date% %time%] [SUCCESS] 依赖安装完成 >> "!LOG_FILE!" 2>&1
) else (
    echo [%date% %time%] [SUCCESS] 所有依赖已安装 >> "!LOG_FILE!" 2>&1
)

:: 第二步：检查文件和目录
if not exist "!SCRIPT_PATH!" (
    echo [%date% %time%] [ERROR] 主启动文件不存在: !SCRIPT_PATH! >> "!LOG_FILE!" 2>&1
    exit /b 1
)
if not exist "!SCRIPT_DIR!eastmoney" (
    echo [%date% %time%] [ERROR] 东方财富模块目录不存在 >> "!LOG_FILE!" 2>&1
    exit /b 1
)
if not exist "!SCRIPT_DIR!xuangubao" (
    echo [%date% %time%] [ERROR] 选股宝模块目录不存在 >> "!LOG_FILE!" 2>&1
    exit /b 1
)

:: 第三步：检查Python环境
if not exist "!PYTHON_EXE!" (
    echo [%date% %time%] [WARNING] pythonw.exe不存在，使用python.exe >> "!LOG_FILE!" 2>&1
    set "PYTHON_EXE=!PYTHON_BACKUP!"
)

:: 第四步：停止残留服务（静默）
if exist "!PID_FILE!" (
    echo [%date% %time%] [INFO] 停止残留服务 >> "!LOG_FILE!" 2>&1
    for /f "delims=" %%i in (!PID_FILE!) do (
        taskkill /f /pid %%i >nul 2>&1
    )
    del /f /q "!PID_FILE!" >nul 2>&1
)

:: 第五步：检查端口占用
netstat -ano | findstr :8000 | findstr LISTENING >nul
if !errorlevel! equ 0 (
    echo [%date% %time%] [ERROR] 8000端口已被占用 >> "!LOG_FILE!" 2>&1
    exit /b 1
)

:: 第六步：彻底后台启动服务（pythonw无窗口，输出到日志）
echo [%date% %time%] [INFO] 启动A股行情API服务 >> "!LOG_FILE!" 2>&1
start "" /b /low "!PYTHON_EXE!" "!SCRIPT_PATH!" >> "!LOG_FILE!" 2>&1

:: 第七步：验证启动状态
timeout /t 8 /nobreak >nul
set "SERVICE_PID="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
    set "SERVICE_PID=%%a"
)

if not "!SERVICE_PID!"=="" (
    echo !SERVICE_PID! > "!PID_FILE!"
    echo [%date% %time%] [SUCCESS] 服务启动成功，PID: !SERVICE_PID! >> "!LOG_FILE!" 2>&1
    echo [%date% %time%] [INFO] 访问地址: http://localhost:8000 >> "!LOG_FILE!" 2>&1
) else (
    echo [%date% %time%] [ERROR] 服务启动失败 >> "!LOG_FILE!" 2>&1
    :: 前台启动一次输出错误（仅日志，无窗口）
    "!PYTHON_BACKUP!" "!SCRIPT_PATH!" >> "!LOG_FILE!" 2>&1
)

echo [%date% %time%] === 启动服务结束 === >> "!LOG_FILE!" 2>&1
endlocal
exit /b 0