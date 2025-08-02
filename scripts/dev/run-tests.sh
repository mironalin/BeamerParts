#!/bin/bash

# Run Tests for All Services
echo "🧪 Running Tests for All BeamerParts Services..."

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

# First, try to run all tests from root if possible
if [ -f "./mvnw" ]; then
    echo -e "${BLUE}🧪 Running all tests from root...${NC}"
    ./mvnw test
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed successfully!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️ Some tests failed in root build, checking individual services...${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ No root Maven wrapper found, testing services individually...${NC}"
fi

# Test shared module first (fallback method)
echo -e "${BLUE}🧪 Testing shared module...${NC}"
cd shared
if [ -f "./mvnw" ]; then
    ./mvnw test
else
    # Use Maven from PATH or use a service's mvnw
    if command -v mvn >/dev/null 2>&1; then
        mvn test
    else
        # Borrow mvnw from user_service
        "$PROJECT_ROOT/user_service/mvnw" -f pom.xml test
    fi
fi
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Shared module tests passed${NC}"
else
    echo -e "${RED}❌ Shared module tests failed${NC}"
    exit 1
fi

# List of services to test
services=("api_gateway" "user_service" "vehicle_service" "product_service" "order_service")

echo -e "\n${BLUE}🧪 Running tests for all services...${NC}"

failed_services=()

for service in "${services[@]}"; do
    echo -e "\n${YELLOW}🧪 Testing $service...${NC}"
    cd "$PROJECT_ROOT/$service"
    
    # Run tests
    ./mvnw test
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $service tests passed${NC}"
    else
        echo -e "${RED}❌ $service tests failed${NC}"
        failed_services+=("$service")
    fi
done

echo -e "\n${BLUE}📊 Test Results Summary:${NC}"

if [ ${#failed_services[@]} -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed successfully!${NC}"
    exit 0
else
    echo -e "${RED}❌ The following services had test failures:${NC}"
    for service in "${failed_services[@]}"; do
        echo -e "${RED}  - $service${NC}"
    done
    exit 1
fi
