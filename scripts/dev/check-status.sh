#!/bin/bash

# Check Status of All Services
echo "📊 BeamerParts Service Status Check"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to check service health
check_service() {
    local url=$1
    local service_name=$2
    local port=$3
    
    # Check if port is open
    if nc -z localhost "$port" 2>/dev/null; then
        # Try to get health endpoint
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is running and healthy ($url)${NC}"
        else
            echo -e "${YELLOW}⚠️  $service_name is running but health check failed ($url)${NC}"
        fi
    else
        echo -e "${RED}❌ $service_name is not running (port $port)${NC}"
    fi
}

echo -e "${BLUE}🌐 API Gateway Routes:${NC}"
check_service "http://localhost:8080/actuator/health" "API Gateway" 8080

echo -e "\n${BLUE}🔧 Individual Services:${NC}"
check_service "http://localhost:8081/actuator/health" "User Service" 8081
check_service "http://localhost:8082/actuator/health" "Vehicle Service" 8082
check_service "http://localhost:8083/actuator/health" "Product Service" 8083
check_service "http://localhost:8084/actuator/health" "Order Service" 8084

echo -e "\n${BLUE}🐳 Infrastructure Services:${NC}"
if nc -z localhost 5433 2>/dev/null; then
    echo -e "${GREEN}✅ PostgreSQL User DB (port 5433)${NC}"
else
    echo -e "${RED}❌ PostgreSQL User DB (port 5433)${NC}"
fi

if nc -z localhost 5434 2>/dev/null; then
    echo -e "${GREEN}✅ PostgreSQL Vehicle DB (port 5434)${NC}"
else
    echo -e "${RED}❌ PostgreSQL Vehicle DB (port 5434)${NC}"
fi

if nc -z localhost 5435 2>/dev/null; then
    echo -e "${GREEN}✅ PostgreSQL Product DB (port 5435)${NC}"
else
    echo -e "${RED}❌ PostgreSQL Product DB (port 5435)${NC}"
fi

if nc -z localhost 6379 2>/dev/null; then
    echo -e "${GREEN}✅ Redis (port 6379)${NC}"
else
    echo -e "${RED}❌ Redis (port 6379)${NC}"
fi

if nc -z localhost 5672 2>/dev/null; then
    echo -e "${GREEN}✅ RabbitMQ (port 5672)${NC}"
else
    echo -e "${RED}❌ RabbitMQ (port 5672)${NC}"
fi

echo -e "\n${BLUE}🧪 API Gateway Test URLs:${NC}"
echo "  Health: http://localhost:8080/actuator/health"
echo "  User Service via Gateway: http://localhost:8080/health/user/actuator/health"
echo "  Product Service via Gateway: http://localhost:8080/health/product/actuator/health"
echo "  Vehicle Service via Gateway: http://localhost:8080/health/vehicle/actuator/health"

# Check if PID files exist to show running processes
echo -e "\n${BLUE}🔢 Running Service PIDs:${NC}"
if [ -d "$PROJECT_ROOT/logs" ]; then
    for pidfile in "$PROJECT_ROOT/logs"/*.pid; do
        if [ -f "$pidfile" ]; then
            service_name=$(basename "$pidfile" .pid)
            pid=$(cat "$pidfile")
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "${GREEN}✅ $service_name (PID: $pid)${NC}"
            else
                echo -e "${RED}❌ $service_name (PID: $pid - not running)${NC}"
            fi
        fi
    done
else
    echo -e "${YELLOW}⚠️  No PID files found. Services may not be started via scripts.${NC}"
fi
