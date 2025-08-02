#!/bin/bash

# Start Vehicle Service
echo "🚗 Starting Vehicle Service on port 8082..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if port 8082 is available
if lsof -i :8082 > /dev/null 2>&1; then
    echo -e "${RED}❌ Port 8082 is already in use${NC}"
    echo "Would you like to kill the process using port 8082? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        lsof -ti :8082 | xargs kill -9
        echo -e "${YELLOW}🔄 Killed process on port 8082${NC}"
    else
        exit 1
    fi
fi

# Navigate to Vehicle Service directory
cd "$PROJECT_ROOT/vehicle_service"

# Build and run
echo -e "${YELLOW}🔨 Building and starting Vehicle Service...${NC}"
./mvnw spring-boot:run

echo -e "${GREEN}✅ Vehicle Service started successfully!${NC}"
