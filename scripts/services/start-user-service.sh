#!/bin/bash

# Start User Service
echo "👤 Starting User Service on port 8081..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if port 8081 is available
if lsof -i :8081 > /dev/null 2>&1; then
    echo -e "${RED}❌ Port 8081 is already in use${NC}"
    echo "Would you like to kill the process using port 8081? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        lsof -ti :8081 | xargs kill -9
        echo -e "${YELLOW}🔄 Killed process on port 8081${NC}"
    else
        exit 1
    fi
fi

# Navigate to User Service directory
cd "$PROJECT_ROOT/user-service"

# Build and run
echo -e "${YELLOW}🔨 Building and starting User Service...${NC}"
./mvnw spring-boot:run

echo -e "${GREEN}✅ User Service started successfully!${NC}"
