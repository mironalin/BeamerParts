#!/bin/bash

# Start Order Service
echo "📋 Starting Order Service on port 8084..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if port 8084 is available
if lsof -i :8084 > /dev/null 2>&1; then
    echo -e "${RED}❌ Port 8084 is already in use${NC}"
    echo "Would you like to kill the process using port 8084? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        lsof -ti :8084 | xargs kill -9
        echo -e "${YELLOW}🔄 Killed process on port 8084${NC}"
    else
        exit 1
    fi
fi

# Navigate to Order Service directory
cd "$PROJECT_ROOT/order_service"

# Build and run
echo -e "${YELLOW}🔨 Building and starting Order Service...${NC}"
./mvnw spring-boot:run

echo -e "${GREEN}✅ Order Service started successfully!${NC}"
