#!/bin/bash

# Stop Docker Infrastructure for BeamerParts
echo "🛑 Stopping BeamerParts Infrastructure..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Navigate to project root
cd "$PROJECT_ROOT"

# Stop infrastructure
echo -e "${YELLOW}🛑 Stopping infrastructure services...${NC}"
docker-compose -f docker-compose.infra.yml down

echo -e "${GREEN}✅ Infrastructure stopped successfully!${NC}"
