#!/bin/bash

# Show Infrastructure Logs
echo "📝 BeamerParts Infrastructure Logs"

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

echo -e "${BLUE}🐳 Docker Infrastructure Logs:${NC}"
echo -e "${YELLOW}Press Ctrl+C to exit log viewing${NC}"
echo ""

# Follow logs for all infrastructure services
docker-compose -f docker-compose.infra.yml logs -f
