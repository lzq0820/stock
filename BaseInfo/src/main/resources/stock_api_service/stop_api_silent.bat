@echo off
:: Stock API服务静默停止脚本（无窗口、无交互）
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1

:: ==================== 配置 ====================
set "SCRIPT_DIR=%~dp0"
set "PID_FILE=!SCRIPT_DIR!stock_api_service.pid"
set "SCRIPT_PATH=!SCRIPT_DIR!main.py"
set "PYTHON_PATH=D:\app\develop\python3\"
set "STOP_LOG=!SCRIPT_DIR!stop_service.log"
:: ==============================================

:: 清空停止日志
echo [%date% %time%] === 停止服务开始 === > "!STOP_LOG!" 2>&1
set "KILLED=0"

:: 第一步：通过PID文件终止
if exist "!PID_FILE!" (
    for /f "delims=" %%i in (!PID_FILE!) do (
        set "TARGET_PID=%%i"
        if "!TARGET_PID!" neq "" (
            echo [%date% %time%] [INFO] 终止PID: !TARGET_PID! >> "!STOP_LOG!" 2>&1
            taskkill /f /pid !TARGET_PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo [%date% %time%] [SUCCESS] 成功终止PID: !TARGET_PID! >> "!STOP_LOG!" 2>&1
                set "KILLED=1"
            ) else (
                echo [%date% %time%] [WARNING] PID !TARGET_PID! 不存在 >> "!STOP_LOG!" 2>&1
            )
        )
    )
    del /f /q !PID_FILE! >nul 2>&1
    echo [%date% %time%] [INFO] 清理PID文件 >> "!STOP_LOG!" 2>&1
) else (
    echo [%date% %time%] [WARNING] PID文件不存在 >> "!STOP_LOG!" 2>&1
)

:: 第二步：通过命令行终止
if !KILLED! equ 0 (
    for /f "tokens=2 delims=," %%a in (
        'wmic process where "name like 'python%.exe' and commandline like '%%!SCRIPT_PATH!%%'" get processid^,commandline /format:csv ^| findstr /i /c:"!SCRIPT_PATH!"'
    ) do (
        if "%%a" neq "ProcessId" if "%%a" neq "" (
            set "CMD_PID=%%a"
            echo [%date% %time%] [INFO] 终止命令行匹配PID: !CMD_PID! >> "!STOP_LOG!" 2>&1
            taskkill /f /pid !CMD_PID! >nul 2>&1
            if !errorlevel! equ 0 (
                echo [%date% %time%] [SUCCESS] 终止PID: !CMD_PID! >> "!STOP_LOG!" 2>&1
                set "KILLED=1"
            )
        )
    )
)

:: 第三步：通过端口终止
if !KILLED! equ 0 (
    echo [%date% %time%] [INFO] 检查8000端口占用 >> "!STOP_LOG!" 2>&1
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING') do (
        set "PORT_PID=%%a"
        taskkill /f /pid !PORT_PID! >nul 2>&1
        if !errorlevel! equ 0 (
            echo [%date% %time%] [SUCCESS] 终止端口8000 PID: !PORT_PID! >> "!STOP_LOG!" 2>&1
            set "KILLED=1"
        )
    )
)

:: 第四步：终极兜底
if !KILLED! equ 0 (
    echo [%date% %time%] [WARNING] 终止指定路径Python进程 >> "!STOP_LOG!" 2>&1
    taskkill /f /im pythonw.exe /fi "imagepath eq !PYTHON_PATH!*" >nul 2>&1
    taskkill /f /im python.exe /fi "imagepath eq !PYTHON_PATH!*" >nul 2>&1
    set "KILLED=1"
)

:: 验证端口释放
timeout /t 3 /nobreak >nul
netstat -ano | findstr :8000 | findstr LISTENING >nul
if !errorlevel! equ 1 (
    echo [%date% %time%] [SUCCESS] 服务已完全停止，端口8000释放 >> "!STOP_LOG!" 2>&1
) else (
    echo [%date% %time%] [ERROR] 服务停止失败，端口8000仍被占用 >> "!STOP_LOG!" 2>&1
    netstat -ano | findstr :8000 >> "!STOP_LOG!" 2>&1
)

echo [%date% %time%] === 停止服务结束 === >> "!STOP_LOG!" 2>&1
endlocal
exit /b 0