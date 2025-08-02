#!/bin/bash

# Show Docker Infrastructure Status and Logs
echo "📊 BeamerParts Infrastructure Status"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Navigate to project root
cd "$PROJECT_ROOT"

echo -e "${BLUE}🐳 Docker Container Status:${NC}"
docker-compose -f docker-compose.infra.yml ps

echo -e "\n${BLUE}📊 Service Health Check:${NC}"

# Function to check service health
check_service() {
    local host=$1
    local port=$2
    local service_name=$3
    
    if nc -z "$host" "$port" 2>/dev/null; then
        echo -e "${GREEN}✅ $service_name is running (${host}:${port})${NC}"
    else
        echo -e "${RED}❌ $service_name is not responding (${host}:${port})${NC}"
    fi
}

check_service localhost 5433 "PostgreSQL User DB"
check_service localhost 5434 "PostgreSQL Vehicle DB"
check_service localhost 5435 "PostgreSQL Product DB"
check_service localhost 6379 "Redis"
check_service localhost 5672 "RabbitMQ"

echo -e "\n${YELLOW}📝 Recent logs (last 10 lines):${NC}"
docker-compose -f docker-compose.infra.yml logs --tail=10
