#!/bin/bash

# Start All Services for Development
echo "🏗️ Starting All BeamerParts Services..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to start service in background
start_service_bg() {
    local service_script=$1
    local service_name=$2
    local log_file=$3
    
    echo -e "${BLUE}🚀 Starting $service_name...${NC}"
    
    # Create logs directory if it doesn't exist
    mkdir -p "$PROJECT_ROOT/logs"
    
    # Start service in background and redirect output to log file
    bash "$service_script" > "$log_file" 2>&1 &
    local pid=$!
    
    # Store PID for later cleanup
    echo $pid > "$PROJECT_ROOT/logs/${service_name,,}.pid"
    
    echo -e "${YELLOW}📝 $service_name logs: $log_file${NC}"
    echo -e "${YELLOW}🔢 $service_name PID: $pid${NC}"
}

# Check if infrastructure is running
echo -e "${BLUE}🔍 Checking infrastructure status...${NC}"
if ! nc -z localhost 5433 2>/dev/null; then
    echo -e "${RED}❌ Infrastructure is not running. Starting infrastructure first...${NC}"
    "$SCRIPT_DIR/../docker/start-infra.sh"
    echo -e "${YELLOW}⏳ Waiting 10 seconds for infrastructure to stabilize...${NC}"
    sleep 10
fi

echo -e "${GREEN}✅ Infrastructure is ready!${NC}"

# Create logs directory
mkdir -p "$PROJECT_ROOT/logs"

# Start all services in background
echo -e "${BLUE}🚀 Starting all services in parallel...${NC}"

start_service_bg "$SCRIPT_DIR/../services/start-user-service.sh" "UserService" "$PROJECT_ROOT/logs/user-service.log"
start_service_bg "$SCRIPT_DIR/../services/start-vehicle-service.sh" "VehicleService" "$PROJECT_ROOT/logs/vehicle-service.log"
start_service_bg "$SCRIPT_DIR/../services/start-product-service.sh" "ProductService" "$PROJECT_ROOT/logs/product-service.log"
start_service_bg "$SCRIPT_DIR/../services/start-order-service.sh" "OrderService" "$PROJECT_ROOT/logs/order-service.log"

# Wait a bit for services to start
echo -e "${YELLOW}⏳ Waiting 30 seconds for services to start...${NC}"
sleep 30

# Start API Gateway last
start_service_bg "$SCRIPT_DIR/../services/start-api-gateway.sh" "APIGateway" "$PROJECT_ROOT/logs/api-gateway.log"

echo -e "${YELLOW}⏳ Waiting 15 seconds for API Gateway to start...${NC}"
sleep 15

echo -e "${GREEN}🎉 All services are starting up!${NC}"
echo -e "${BLUE}📊 Service URLs:${NC}"
echo "  API Gateway:    http://localhost:8080"
echo "  User Service:   http://localhost:8081"
echo "  Vehicle Service: http://localhost:8082"
echo "  Product Service: http://localhost:8083"
echo "  Order Service:  http://localhost:8084"
echo ""
echo -e "${YELLOW}📝 Log files are in: $PROJECT_ROOT/logs/${NC}"
echo -e "${YELLOW}🔧 Use 'scripts/dev/check-status.sh' to check service health${NC}"
echo -e "${YELLOW}🛑 Use 'scripts/dev/stop-all.sh' to stop all services${NC}"
