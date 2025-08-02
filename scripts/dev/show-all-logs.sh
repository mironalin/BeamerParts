#!/bin/bash

# Show All Logs (Infrastructure + Services)
echo "📝 All BeamerParts Logs"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📝 Showing all logs (Infrastructure + Services)${NC}"
echo -e "${YELLOW}Press Ctrl+C to exit${NC}"
echo ""

# Navigate to project root
cd "$PROJECT_ROOT"

# Show infrastructure logs in background
echo -e "${BLUE}🐳 Infrastructure Logs:${NC}"
docker-compose -f docker-compose.infra.yml logs --tail=20

echo -e "\n${BLUE}🏗️ Service Logs:${NC}"
if [ -d "$PROJECT_ROOT/logs" ]; then
    for logfile in "$PROJECT_ROOT/logs"/*.log; do
        if [ -f "$logfile" ]; then
            echo -e "\n${YELLOW}=== $(basename "$logfile") (last 20 lines) ===${NC}"
            tail -20 "$logfile"
        fi
    done
    
    echo -e "\n${BLUE}📝 Following all logs (Press Ctrl+C to exit):${NC}"
    
    # Start following infrastructure logs in background
    docker-compose -f docker-compose.infra.yml logs -f &
    infra_pid=$!
    
    # Follow service logs
    tail -f "$PROJECT_ROOT/logs"/*.log 2>/dev/null &
    service_pid=$!
    
    # Wait for user to press Ctrl+C
    trap "kill $infra_pid $service_pid 2>/dev/null; exit 0" INT
    wait
else
    echo -e "${YELLOW}⚠️  No service logs found. Services may not be running via scripts.${NC}"
    echo -e "${BLUE}📝 Following infrastructure logs only:${NC}"
    docker-compose -f docker-compose.infra.yml logs -f
fi
