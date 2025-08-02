#!/bin/bash

# Start Docker Infrastructure for BeamerParts
echo "🐳 Starting BeamerParts Infrastructure (PostgreSQL, Redis, RabbitMQ)..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Navigate to project root
cd "$PROJECT_ROOT"

# Start infrastructure
echo -e "${YELLOW}🏗️ Starting infrastructure services...${NC}"
docker-compose -f docker-compose.infra.yml up -d

# Wait for services to be ready
echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"

# Function to wait for a service
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z "$host" "$port" 2>/dev/null; then
            echo -e "${GREEN}✅ $service_name is ready${NC}"
            return 0
        fi
        echo -e "${YELLOW}⏳ Waiting for $service_name (attempt $attempt/$max_attempts)...${NC}"
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}❌ $service_name failed to start within timeout${NC}"
    return 1
}

# Wait for all services
wait_for_service localhost 5433 "PostgreSQL (User DB)"
wait_for_service localhost 5434 "PostgreSQL (Vehicle DB)"
wait_for_service localhost 5435 "PostgreSQL (Product DB)"
wait_for_service localhost 6379 "Redis"
wait_for_service localhost 5672 "RabbitMQ"

echo -e "${GREEN}🎉 All infrastructure services are ready!${NC}"
echo -e "${YELLOW}📊 Service URLs:${NC}"
echo "  PostgreSQL User DB:    localhost:5433"
echo "  PostgreSQL Vehicle DB: localhost:5434"
echo "  PostgreSQL Product DB: localhost:5435"
echo "  Redis:                 localhost:6379"
echo "  RabbitMQ:              localhost:5672"
echo "  RabbitMQ Management:   http://localhost:15672 (guest/guest)"
