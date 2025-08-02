#!/bin/bash

# Stop All Services
echo "🛑 Stopping All BeamerParts Services..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔍 Looking for running services...${NC}"

# Function to kill processes on specific ports
kill_port() {
    local port=$1
    local service_name=$2
    
    if lsof -i :"$port" > /dev/null 2>&1; then
        echo -e "${YELLOW}🛑 Stopping $service_name on port $port...${NC}"
        lsof -ti :"$port" | xargs kill -9 2>/dev/null
        echo -e "${GREEN}✅ $service_name stopped${NC}"
    else
        echo -e "${BLUE}ℹ️  $service_name was not running on port $port${NC}"
    fi
}

# Stop services by port
kill_port 8080 "API Gateway"
kill_port 8081 "User Service"
kill_port 8082 "Vehicle Service"
kill_port 8083 "Product Service"
kill_port 8084 "Order Service"

# Stop services by PID files if they exist
echo -e "\n${BLUE}🔍 Checking for PID files...${NC}"
if [ -d "$PROJECT_ROOT/logs" ]; then
    for pidfile in "$PROJECT_ROOT/logs"/*.pid; do
        if [ -f "$pidfile" ]; then
            service_name=$(basename "$pidfile" .pid)
            pid=$(cat "$pidfile")
            
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "${YELLOW}🛑 Stopping $service_name (PID: $pid)...${NC}"
                kill -9 "$pid" 2>/dev/null
                echo -e "${GREEN}✅ $service_name stopped${NC}"
            else
                echo -e "${BLUE}ℹ️  $service_name (PID: $pid) was already stopped${NC}"
            fi
            
            # Remove PID file
            rm "$pidfile"
        fi
    done
else
    echo -e "${BLUE}ℹ️  No PID files found${NC}"
fi

# Stop Docker infrastructure
echo -e "\n${BLUE}🐳 Stopping Docker infrastructure...${NC}"
"$SCRIPT_DIR/../docker/stop-infra.sh"

# Clean up log files (optional)
echo -e "\n${YELLOW}🧹 Cleaning up log files...${NC}"
if [ -d "$PROJECT_ROOT/logs" ]; then
    rm -rf "$PROJECT_ROOT/logs"/*.log
    echo -e "${GREEN}✅ Log files cleaned${NC}"
fi

echo -e "\n${GREEN}🎉 All services stopped successfully!${NC}"
