#!/bin/bash

# Show Service Logs
echo "📝 BeamerParts Service Logs"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if logs directory exists
if [ ! -d "$PROJECT_ROOT/logs" ]; then
    echo -e "${RED}❌ No logs directory found. Services may not be running via scripts.${NC}"
    exit 1
fi

echo -e "${BLUE}📝 Available service logs:${NC}"
echo "1) API Gateway"
echo "2) User Service"
echo "3) Vehicle Service"
echo "4) Product Service"
echo "5) Order Service"
echo "6) All Services (combined)"
echo "7) Tail all services (follow)"

read -p "Select option [1-7]: " choice

case $choice in
    1)
        if [ -f "$PROJECT_ROOT/logs/api-gateway.log" ]; then
            echo -e "${BLUE}🌐 API Gateway Logs:${NC}"
            cat "$PROJECT_ROOT/logs/api-gateway.log"
        else
            echo -e "${RED}❌ API Gateway log file not found${NC}"
        fi
        ;;
    2)
        if [ -f "$PROJECT_ROOT/logs/user-service.log" ]; then
            echo -e "${BLUE}👤 User Service Logs:${NC}"
            cat "$PROJECT_ROOT/logs/user-service.log"
        else
            echo -e "${RED}❌ User Service log file not found${NC}"
        fi
        ;;
    3)
        if [ -f "$PROJECT_ROOT/logs/vehicle-service.log" ]; then
            echo -e "${BLUE}🚗 Vehicle Service Logs:${NC}"
            cat "$PROJECT_ROOT/logs/vehicle-service.log"
        else
            echo -e "${RED}❌ Vehicle Service log file not found${NC}"
        fi
        ;;
    4)
        if [ -f "$PROJECT_ROOT/logs/product-service.log" ]; then
            echo -e "${BLUE}📦 Product Service Logs:${NC}"
            cat "$PROJECT_ROOT/logs/product-service.log"
        else
            echo -e "${RED}❌ Product Service log file not found${NC}"
        fi
        ;;
    5)
        if [ -f "$PROJECT_ROOT/logs/order-service.log" ]; then
            echo -e "${BLUE}📋 Order Service Logs:${NC}"
            cat "$PROJECT_ROOT/logs/order-service.log"
        else
            echo -e "${RED}❌ Order Service log file not found${NC}"
        fi
        ;;
    6)
        echo -e "${BLUE}📝 All Service Logs:${NC}"
        for logfile in "$PROJECT_ROOT/logs"/*.log; do
            if [ -f "$logfile" ]; then
                echo -e "\n${YELLOW}=== $(basename "$logfile") ===${NC}"
                cat "$logfile"
            fi
        done
        ;;
    7)
        echo -e "${BLUE}📝 Following all service logs (Press Ctrl+C to exit):${NC}"
        tail -f "$PROJECT_ROOT/logs"/*.log 2>/dev/null
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        ;;
esac
