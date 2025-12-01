@echo off
chcp 65001
echo ========================================
echo   校园新闻发布系统 - 前端启动脚本
echo ========================================
echo.

cd frontend

echo [1/3] 检查环境...
call node -v
if %errorlevel% neq 0 (
    echo 错误: 未检测到Node.js环境，请安装Node.js 16+
    pause
    exit /b 1
)

echo.
echo [2/3] 安装依赖...
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo 错误: 依赖安装失败
        pause
        exit /b 1
    )
)

echo.
echo [3/3] 启动开发服务器...
echo 前端服务将在 http://localhost:3000 启动
echo.
call npm run dev

pause
