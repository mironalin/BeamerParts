@echo off
echo.
echo ============================================================
echo   BeamerParts Microservices - Cloud-Based Startup
echo ============================================================
echo.

REM Check if .env file exists
if not exist .env (
    echo ❌ ERROR: .env file not found!
    echo.
    echo Please copy .env.template to .env and configure your cloud services:
    echo   1. Neon PostgreSQL connection strings
    echo   2. Redis Cloud connection details  
    echo   3. CloudAMQP connection details
    echo.
    echo Example:
    echo   copy .env.template .env
    echo   notepad .env
    echo.
    pause
    exit /b 1
)

echo 📋 Loading environment variables from .env file...
for /f "usebackq tokens=1,2 delims==" %%i in (.env) do (
    REM Skip empty lines and comments
    if not "%%i"=="" if not "%%i:~0,1%"=="#" (
        set "%%i=%%j"
        echo   ✅ %%i
    )
)
echo.

echo 🌐 Starting BeamerParts services with cloud infrastructure...
echo.
echo Services will use:
echo   📊 Databases: Neon PostgreSQL (serverless)
echo   🔄 Caching: Redis Cloud/Upstash
echo   📨 Messaging: CloudAMQP
echo.

REM Start services in separate windows
echo 🚀 Starting User Service...
start "User Service" cmd /c "set NEON_USER_DB_URL=%NEON_USER_DB_URL% && set REDIS_HOST=%REDIS_HOST% && set REDIS_PORT=%REDIS_PORT% && set REDIS_PASSWORD=%REDIS_PASSWORD% && set RABBITMQ_HOST=%RABBITMQ_HOST% && set RABBITMQ_USERNAME=%RABBITMQ_USERNAME% && set RABBITMQ_PASSWORD=%RABBITMQ_PASSWORD% && set JWT_SECRET=%JWT_SECRET% && cd user-service && mvn spring-boot:run -Dspring.profiles.active=neon"

echo 🚗 Starting Vehicle Service...
start "Vehicle Service" cmd /c "set NEON_VEHICLE_DB_URL=%NEON_VEHICLE_DB_URL% && set REDIS_HOST=%REDIS_HOST% && set REDIS_PORT=%REDIS_PORT% && set REDIS_PASSWORD=%REDIS_PASSWORD% && set RABBITMQ_HOST=%RABBITMQ_HOST% && set RABBITMQ_USERNAME=%RABBITMQ_USERNAME% && set RABBITMQ_PASSWORD=%RABBITMQ_PASSWORD% && cd vehicle-service && mvn spring-boot:run -Dspring.profiles.active=neon"

echo 📦 Starting Product Service...
start "Product Service" cmd /c "set NEON_PRODUCT_DB_URL=%NEON_PRODUCT_DB_URL% && set REDIS_HOST=%REDIS_HOST% && set REDIS_PORT=%REDIS_PORT% && set REDIS_PASSWORD=%REDIS_PASSWORD% && set RABBITMQ_HOST=%RABBITMQ_HOST% && set RABBITMQ_USERNAME=%RABBITMQ_USERNAME% && set RABBITMQ_PASSWORD=%RABBITMQ_PASSWORD% && set VEHICLE_SERVICE_URL=%VEHICLE_SERVICE_URL% && cd product-service && mvn spring-boot:run -Dspring.profiles.active=neon"

echo 🌐 Starting API Gateway...
start "API Gateway" cmd /c "set USER_SERVICE_URL=%USER_SERVICE_URL% && set VEHICLE_SERVICE_URL=%VEHICLE_SERVICE_URL% && set PRODUCT_SERVICE_URL=%PRODUCT_SERVICE_URL% && cd api-gateway && mvn spring-boot:run"

echo.
echo ✅ All services are starting in separate windows...
echo.
echo 📍 Service URLs:
echo   🌐 API Gateway:     http://localhost:8080
echo   👤 User Service:    http://localhost:8081  
echo   🚗 Vehicle Service: http://localhost:8082
echo   📦 Product Service: http://localhost:8083
echo.
echo 💡 Tips:
echo   - Check individual service windows for startup logs
echo   - Wait for all services to show "Started Application" 
echo   - Use Ctrl+C in each window to stop services
echo.
echo 📖 Documentation: README-neon.md
echo.
pause
