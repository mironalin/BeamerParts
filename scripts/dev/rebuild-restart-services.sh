#!/bin/bash

# Rebuild and Restart Spring Microservices
# This script stops, rebuilds, and restarts the Spring microservices while keeping infrastructure running

set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}🔄 BeamerParts - Rebuild & Restart Spring Services${NC}"
echo -e "${YELLOW}This will rebuild and restart all Spring microservices while keeping infrastructure running${NC}"
echo ""

# Function to stop service by PID
stop_service() {
    local service_name=$1
    local service_name_lower=$(echo "$service_name" | tr '[:upper:]' '[:lower:]')
    local pid_file="$PROJECT_ROOT/logs/${service_name_lower}.pid"
    
    if [[ -f "$pid_file" ]]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}🛑 Stopping $service_name (PID: $pid)...${NC}"
            kill $pid
            sleep 2
            # Force kill if still running
            if ps -p $pid > /dev/null 2>&1; then
                echo -e "${RED}🔨 Force stopping $service_name...${NC}"
                kill -9 $pid
            fi
        else
            echo -e "${YELLOW}⚠️  $service_name PID file exists but process not running${NC}"
        fi
        rm -f "$pid_file"
    else
        echo -e "${YELLOW}⚠️  No PID file found for $service_name${NC}"
    fi
}

# Function to stop service by port
stop_service_by_port() {
    local service_name=$1
    local port=$2
    
    echo -e "${YELLOW}🔍 Checking port $port for $service_name...${NC}"
    if lsof -i :$port > /dev/null 2>&1; then
        echo -e "${YELLOW}🛑 Stopping process on port $port...${NC}"
        lsof -ti :$port | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
}

# Function to build service
build_service() {
    local service_dir=$1
    local service_name=$2
    
    echo -e "${BLUE}🔨 Building $service_name...${NC}"
    cd "$PROJECT_ROOT/$service_dir"
    
    # Check if mvnw exists, if not use parent mvnw
    if [[ -f "./mvnw" ]]; then
        # Clean and compile (skip tests for faster build)
        ./mvnw clean compile -DskipTests
    else
        # Use parent mvnw for shared library
        echo -e "${YELLOW}📝 Using parent Maven wrapper for $service_name...${NC}"
        cd "$PROJECT_ROOT"
        ./mvnw clean compile -DskipTests -pl "$service_dir"
        cd "$PROJECT_ROOT/$service_dir"
    fi
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ $service_name built successfully${NC}"
    else
        echo -e "${RED}❌ Failed to build $service_name${NC}"
        return 1
    fi
}

# Function to start service in background
start_service_bg() {
    local service_dir=$1
    local service_name=$2
    local port=$3
    
    echo -e "${BLUE}🚀 Starting $service_name on port $port...${NC}"
    
    cd "$PROJECT_ROOT/$service_dir"
    
    # Create logs directory if it doesn't exist
    mkdir -p "$PROJECT_ROOT/logs"
    
    # Start service in background
    local service_name_lower=$(echo "$service_name" | tr '[:upper:]' '[:lower:]')
    local log_file="$PROJECT_ROOT/logs/${service_name_lower}.log"
    ./mvnw spring-boot:run > "$log_file" 2>&1 &
    local pid=$!
    
    # Store PID
    echo $pid > "$PROJECT_ROOT/logs/${service_name_lower}.pid"
    
    echo -e "${GREEN}✅ $service_name started (PID: $pid)${NC}"
    echo -e "${YELLOW}📝 Logs: $log_file${NC}"
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to start within $(($max_attempts * 2)) seconds${NC}"
    return 1
}

# Check if infrastructure is running
echo -e "${BLUE}🔍 Checking infrastructure status...${NC}"
if ! nc -z localhost 5433 2>/dev/null || ! nc -z localhost 6379 2>/dev/null; then
    echo -e "${RED}❌ Infrastructure is not running. Please start infrastructure first:${NC}"
    echo -e "${YELLOW}   ./scripts/docker/start-infra.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Infrastructure is running${NC}"

# Phase 1: Stop all services
echo -e "${PURPLE}📋 Phase 1: Stopping services...${NC}"
stop_service "API-Gateway"
stop_service "User-Service" 
stop_service "Vehicle-Service"
stop_service "Product-Service"
stop_service "Order-Service"

# Also stop by port as backup
stop_service_by_port "API Gateway" 8080
stop_service_by_port "User Service" 8081
stop_service_by_port "Vehicle Service" 8082
stop_service_by_port "Product Service" 8083
stop_service_by_port "Order Service" 8084

echo -e "${GREEN}✅ All services stopped${NC}"
echo ""

# Phase 2: Build all services
echo -e "${PURPLE}📋 Phase 2: Building services...${NC}"

# Build from project root to ensure proper dependency order
echo -e "${BLUE}🔨 Building all modules from project root...${NC}"
cd "$PROJECT_ROOT"
./mvnw clean compile -DskipTests

if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✅ All services built successfully${NC}"
else
    echo -e "${RED}❌ Failed to build services${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All services built successfully${NC}"
echo ""

# Phase 3: Start services in dependency order
echo -e "${PURPLE}📋 Phase 3: Starting services...${NC}"

# Start core services first
start_service_bg "user_service" "User-Service" 8081
wait_for_service "User Service" 8081

start_service_bg "vehicle_service" "Vehicle-Service" 8082  
wait_for_service "Vehicle Service" 8082

start_service_bg "product_service" "Product-Service" 8083
wait_for_service "Product Service" 8083

# start_service_bg "order_service" "Order-Service" 8084
# wait_for_service "Order Service" 8084

# Start API Gateway last
start_service_bg "api_gateway" "API-Gateway" 8080
wait_for_service "API Gateway" 8080

echo ""
echo -e "${GREEN}🎉 All services rebuilt and restarted successfully!${NC}"
echo -e "${BLUE}📊 Service Status:${NC}"
echo -e "${CYAN}• User Service:    http://localhost:8081/actuator/health${NC}"
echo -e "${CYAN}• Vehicle Service: http://localhost:8082/actuator/health${NC}" 
echo -e "${CYAN}• Product Service: http://localhost:8083/actuator/health${NC}"
echo -e "${CYAN}• Order Service:   http://localhost:8084/actuator/health${NC}"
echo -e "${CYAN}• API Gateway:     http://localhost:8080/actuator/health${NC}"
echo ""
echo -e "${YELLOW}📝 Logs are available in: $PROJECT_ROOT/logs/${NC}"
echo -e "${YELLOW}💡 Use './scripts/dev/show-service-logs.sh' to view logs${NC}"
