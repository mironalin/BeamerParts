#!/bin/bash

# Quick Restart Individual Service
# This script allows you to quickly restart a single service without touching others

set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Function to show available services
show_services() {
    echo -e "${CYAN}Available services:${NC}"
    echo -e "${GREEN}1)${NC} User Service (port 8081)"
    echo -e "${GREEN}2)${NC} Vehicle Service (port 8082)"
    echo -e "${GREEN}3)${NC} Product Service (port 8083)"
    echo -e "${GREEN}4)${NC} Order Service (port 8084)"
    echo -e "${GREEN}5)${NC} API Gateway (port 8080)"
    echo ""
}

# Function to restart service
restart_service() {
    local service_dir=$1
    local service_name=$2
    local port=$3
    
    echo -e "${BLUE}🔄 Restarting $service_name...${NC}"
    
    # Stop service
    local service_name_lower=$(echo "$service_name" | tr '[:upper:]' '[:lower:]')
    local pid_file="$PROJECT_ROOT/logs/${service_name_lower}-service.pid"
    if [[ -f "$pid_file" ]]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}🛑 Stopping $service_name (PID: $pid)...${NC}"
            kill $pid
            sleep 2
        fi
        rm -f "$pid_file"
    fi
    
    # Stop by port as backup
    if lsof -i :$port > /dev/null 2>&1; then
        echo -e "${YELLOW}🛑 Force stopping process on port $port...${NC}"
        lsof -ti :$port | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
    
    # Build service
    echo -e "${BLUE}🔨 Building $service_name...${NC}"
    cd "$PROJECT_ROOT/$service_dir"
    ./mvnw clean compile -DskipTests
    
    if [[ $? -ne 0 ]]; then
        echo -e "${RED}❌ Failed to build $service_name${NC}"
        return 1
    fi
    
    # Start service
    echo -e "${BLUE}🚀 Starting $service_name on port $port...${NC}"
    mkdir -p "$PROJECT_ROOT/logs"
    
    local log_file="$PROJECT_ROOT/logs/${service_name_lower}-service.log"
    ./mvnw spring-boot:run > "$log_file" 2>&1 &
    local pid=$!
    
    echo $pid > "$PROJECT_ROOT/logs/${service_name_lower}-service.pid"
    
    # Wait for service to be ready
    echo -e "${YELLOW}⏳ Waiting for $service_name to be ready...${NC}"
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready! (PID: $pid)${NC}"
            echo -e "${YELLOW}📝 Logs: $log_file${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to start within $(($max_attempts * 2)) seconds${NC}"
    return 1
}

# Main script
echo -e "${CYAN}🔄 BeamerParts - Quick Service Restart${NC}"
echo ""

if [[ $# -eq 1 ]]; then
    # Service specified as argument
    case $1 in
        "user"|"1") restart_service "user-service" "User" 8081 ;;
        "vehicle"|"2") restart_service "vehicle-service" "Vehicle" 8082 ;;
        "product"|"3") restart_service "product-service" "Product" 8083 ;;
        "order"|"4") restart_service "order-service" "Order" 8084 ;;
        "gateway"|"api"|"5") restart_service "api-gateway" "API-Gateway" 8080 ;;
        *) echo -e "${RED}❌ Unknown service: $1${NC}"; exit 1 ;;
    esac
else
    # Interactive mode
    show_services
    read -p "Select service to restart [1-5]: " choice
    
    case $choice in
        1) restart_service "user-service" "User" 8081 ;;
        2) restart_service "vehicle-service" "Vehicle" 8082 ;;
        3) restart_service "product-service" "Product" 8083 ;;
        4) restart_service "order-service" "Order" 8084 ;;
        5) restart_service "api-gateway" "API-Gateway" 8080 ;;
        *) echo -e "${RED}❌ Invalid option${NC}"; exit 1 ;;
    esac
fi

echo -e "${GREEN}🎉 Service restart completed!${NC}"
