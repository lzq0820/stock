@echo off
:: A股行情API服务一键启动脚本（带依赖检查，默认自动安装）
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1

:: ==================== 核心配置 ====================
set "SCRIPT_DIR=%~dp0"
set "PYTHON_EXE=D:\app\develop\python3\pythonw.exe"
set "PYTHON_BACKUP=D:\app\develop\python3\python.exe"
set "SCRIPT_PATH=!SCRIPT_DIR!main.py"
set "LOG_FILE=!SCRIPT_DIR!stock_api_service.log"
set "PID_FILE=!SCRIPT_DIR!stock_api_service.pid"

:: 修复：用引号包裹依赖列表，避免>=符号被拆分
set "REQUIRED_PACKAGES=akshare>=1.10.0 pandas>=1.5.0 fastapi>=0.104.1 uvicorn>=0.24.0 requests>=2.31.0 tenacity>=8.2.3 urllib3>=2.0.7"
:: 临时文件用于存储拆分后的依赖（解决>=解析问题）
set "TEMP_DEP_FILE=!SCRIPT_DIR!temp_deps.txt"
:: ==================================================

:: 第一步：修复依赖检查逻辑（解决>=符号拆分问题）
echo [INFO] 开始检查Python依赖...
set "MISSING_PACKAGES="

:: 修复：将依赖列表写入临时文件，按空格拆分（避免>=被解析为分隔符）
echo !REQUIRED_PACKAGES! > "!TEMP_DEP_FILE!"
for /f "tokens=1-7" %%a in (!TEMP_DEP_FILE!) do (
    set "DEP1=%%a"
    set "DEP2=%%b"
    set "DEP3=%%c"
    set "DEP4=%%d"
    set "DEP5=%%e"
    set "DEP6=%%f"
    set "DEP7=%%g"

    :: 定义所有依赖数组
    set "DEPS[1]=!DEP1!"
    set "DEPS[2]=!DEP2!"
    set "DEPS[3]=!DEP3!"
    set "DEPS[4]=!DEP4!"
    set "DEPS[5]=!DEP5!"
    set "DEPS[6]=!DEP6!"
    set "DEPS[7]=!DEP7!"
)
:: 删除临时文件
del /f /q "!TEMP_DEP_FILE!" >nul 2>&1

:: 遍历所有依赖检查是否安装
for /l %%i in (1,1,7) do (
    set "DEP=!DEPS[%%i]!"
    if "!DEP!" neq "" (
        :: 提取包名（去掉版本号和>=）
        for /f "delims=>= " %%n in ("!DEP!") do set "PKG_NAME=%%n"
        :: 检查包是否安装
        "!PYTHON_BACKUP!" -c "import %%n" >nul 2>&1
        if !errorlevel! neq 0 (
            set "MISSING_PACKAGES=!MISSING_PACKAGES! !DEP!"
        )
    )
)

:: 如果有缺失的依赖，默认自动安装（无需手动输入Y）
if not "!MISSING_PACKAGES!"=="" (
    echo [WARNING] 检测到缺失以下依赖包：
    echo !MISSING_PACKAGES!
    echo.
    :: 修复：默认Y，直接安装，无需交互
    set "INSTALL_CHOICE=Y"
    echo [INFO] 默认选择自动安装依赖（Y）...
    if /i "!INSTALL_CHOICE!"=="Y" (
        echo [INFO] 开始安装依赖包（使用清华镜像源）...
        "!PYTHON_BACKUP!" -m pip install !REQUIRED_PACKAGES! -i https://pypi.tuna.tsinghua.edu.cn/simple
        if !errorlevel! neq 0 (
            echo [ERROR] 依赖安装失败，请手动执行以下命令：
            echo pip install !REQUIRED_PACKAGES!
            pause
            exit /b 1
        )
        echo [SUCCESS] 依赖安装完成！
        echo.
    ) else (
        echo [INFO] 跳过依赖安装，请注意：缺失依赖可能导致服务无法正常运行！
        echo 手动安装命令：pip install !REQUIRED_PACKAGES!
        echo.
    )
) else (
    echo [SUCCESS] 所有依赖包已安装完成！
    echo.
)

:: 第二步：原有启动逻辑（略作保留）
:: 1. 检查主启动文件是否存在
if not exist "!SCRIPT_PATH!" (
    echo [ERROR] 主启动文件不存在: !SCRIPT_PATH!
    pause
    exit /b 1
)

:: 2. 检查子模块目录
if not exist "!SCRIPT_DIR!eastmoney" (
    echo [ERROR] 东方财富模块目录不存在: !SCRIPT_DIR!eastmoney
    pause
    exit /b 1
)
if not exist "!SCRIPT_DIR!xuangubao" (
    echo [ERROR] 选股宝模块目录不存在: !SCRIPT_DIR!xuangubao
    pause
    exit /b 1
)

:: 3. 检查Python环境
if not exist "!PYTHON_EXE!" (
    echo [WARNING] pythonw.exe不存在，使用python.exe启动
    set "PYTHON_EXE=!PYTHON_BACKUP!"
)

:: 4. 停止残留服务
if exist "!PID_FILE!" (
    echo [INFO] 停止残留服务...
    for /f "delims=" %%i in (!PID_FILE!) do (
        taskkill /f /pid %%i >nul 2>&1
    )
    del /f /q "!PID_FILE!" >nul 2>&1
)

:: 5. 检查8000端口占用
netstat -ano | findstr :8000 | findstr LISTENING >nul
if !errorlevel! equ 0 (
    echo [ERROR] 8000端口已被占用，请先释放端口后重试
    pause
    exit /b 1
)

:: 6. 后台启动服务
echo [INFO] 启动A股行情API服务...
start "" /b /low "!PYTHON_EXE!" "!SCRIPT_PATH!" > "!LOG_FILE!" 2>&1

:: 7. 等待服务启动
timeout /t 8 /nobreak >nul

:: 8. 验证启动状态
set "SERVICE_PID="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
    set "SERVICE_PID=%%a"
)

:: 9. 输出启动结果
if not "!SERVICE_PID!"=="" (
    echo !SERVICE_PID! > "!PID_FILE!"
    echo ==============================================
    echo [SUCCESS] A股行情API服务启动成功
    echo 服务PID: !SERVICE_PID!
    echo 访问地址: http://localhost:8000
    echo 接口文档: http://localhost:8000/docs
    echo 日志文件: !LOG_FILE!
    echo ==============================================
) else (
    echo [ERROR] A股行情API服务启动失败
    echo 请查看日志文件排查问题: !LOG_FILE!
    echo 正在前台启动以显示错误信息...
    "!PYTHON_BACKUP!" "!SCRIPT_PATH!"
    pause
)

endlocal
exit /b 0