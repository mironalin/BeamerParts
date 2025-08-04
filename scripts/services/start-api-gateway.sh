#!/bin/bash

# Start API Gateway
echo "🌐 Starting API Gateway on port 8080..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if port 8080 is available
if lsof -i :8080 > /dev/null 2>&1; then
    echo -e "${RED}❌ Port 8080 is already in use${NC}"
    echo "Would you like to kill the process using port 8080? (y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        lsof -ti :8080 | xargs kill -9
        echo -e "${YELLOW}🔄 Killed process on port 8080${NC}"
    else
        exit 1
    fi
fi

# Navigate to API Gateway directory
cd "$PROJECT_ROOT/api-gateway"

# Build and run
echo -e "${YELLOW}🔨 Building and starting API Gateway...${NC}"
./mvnw spring-boot:run

echo -e "${GREEN}✅ API Gateway started successfully!${NC}"
