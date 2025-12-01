@echo off
chcp 65001
echo ========================================
echo   校园新闻发布系统 - 后端启动脚本
echo ========================================
echo.

cd backend

echo [1/3] 检查环境...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未检测到Java环境，请安装JDK 17+
    pause
    exit /b 1
)

echo.
echo [2/3] 编译项目...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 错误: 编译失败，请检查代码
    pause
    exit /b 1
)

echo.
echo [3/3] 启动服务...
echo 后端服务将在 http://localhost:8080/api 启动
echo Swagger文档: http://localhost:8080/api/swagger-ui.html
echo.
java -jar target/campus-news-system-1.0.0.jar

pause
