@echo off
:: 关键：启用延迟扩展，确保变量正常解析
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1

:: ==================== 配置（和启动脚本完全一致） ====================
set "SCRIPT_DIR=%~dp0"
set "PID_FILE=!SCRIPT_DIR!stock_api_service.pid"  # 和启动脚本的PID文件名称一致
set "SCRIPT_PATH=!SCRIPT_DIR!main.py"             # 主启动文件路径
set "PYTHON_PATH=D:\app\develop\python3\"        # Python安装路径（和启动脚本一致）
:: ==============================================================

echo ==============================================
echo            Stopping Stock API Service
echo ==============================================
set "KILLED=0"

:: 第一步：通过PID文件终止（优先，最精准）
if exist "!PID_FILE!" (
    for /f "delims=" %%i in (!PID_FILE!) do (
        set "TARGET_PID=%%i"
        if "!TARGET_PID!" neq "" (
            echo [INFO] Trying to kill PID: !TARGET_PID! (from PID file)
            taskkill /f /pid !TARGET_PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo [SUCCESS] Killed process ID: !TARGET_PID!
                set "KILLED=1"
            ) else (
                echo [WARNING] PID !TARGET_PID! not found or already stopped
            )
        )
    )
    :: 删除PID文件（无论是否成功终止，都清理）
    del /f /q !PID_FILE! >nul 2>&1
    echo [INFO] Cleaned PID file: !PID_FILE!
) else (
    echo [WARNING] PID file not found: !PID_FILE!
    echo [INFO] Try to find process by command line...
)

:: 第二步：通过进程命令行精准终止（核心逻辑，防止PID文件丢失）
if !KILLED! equ 0 (
    :: 查找运行main.py的python/pythonw进程（精准匹配启动脚本）
    for /f "tokens=2 delims=," %%a in (
        'wmic process where "name like 'python%.exe' and commandline like '%%!SCRIPT_PATH!%%'" get processid^,commandline /format:csv ^| findstr /i /c:"!SCRIPT_PATH!"'
    ) do (
        :: 过滤掉空值和标题行
        if "%%a" neq "ProcessId" if "%%a" neq "" (
            set "CMD_PID=%%a"
            echo [INFO] Found process by command line: !CMD_PID! (running main.py)
            taskkill /f /pid !CMD_PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo [SUCCESS] Killed process ID: !CMD_PID! (by command line)
                set "KILLED=1"
            ) else (
                echo [WARNING] Failed to kill PID !CMD_PID! (command line match)
            )
        )
    )
)

:: 第三步：通过端口兜底终止（8000端口，和启动脚本一致）
if !KILLED! equ 0 (
    echo [INFO] Try to find process by port 8000...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
        set "PORT_PID=%%a"
        if "!PORT_PID!" neq "" (
            echo [INFO] Found process occupying port 8000: !PORT_PID!
            taskkill /f /pid !PORT_PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo [SUCCESS] Killed process ID: !PORT_PID! (port 8000)
                set "KILLED=1"
            ) else (
                echo [WARNING] Failed to kill PID !PORT_PID! (port 8000)
            )
        )
    )
)

:: 第四步：终极兜底（仅终止指定Python路径下的进程，避免误杀其他Python程序）
if !KILLED! equ 0 (
    echo [WARNING] Try to kill all related Python processes under: !PYTHON_PATH!
    :: 终止pythonw.exe（后台运行的进程）
    taskkill /f /im pythonw.exe /fi "imagepath eq !PYTHON_PATH!*" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [INFO] Killed pythonw.exe processes under !PYTHON_PATH!
        set "KILLED=1"
    )
    :: 终止python.exe（前台运行的进程）
    taskkill /f /im python.exe /fi "imagepath eq !PYTHON_PATH!*" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [INFO] Killed python.exe processes under !PYTHON_PATH!
        set "KILLED=1"
    )
)

:: 最终验证：检查8000端口是否释放
timeout /t 3 /nobreak >nul
netstat -ano | findstr :8000 | findstr LISTENING >nul
if !errorlevel! equ 1 (
    echo ==============================================
    echo [SUCCESS] Stock API Service stopped completely!
    echo [INFO] Port 8000 is released
    echo ==============================================
) else (
    echo ==============================================
    echo [ERROR] Failed to stop Stock API Service!
    echo [ERROR] Port 8000 is still occupied by:
    netstat -ano | findstr :8000
    echo.
    echo Please manually kill the above process ID(s)
    echo ==============================================
)

pause
endlocal