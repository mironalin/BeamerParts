#!/bin/bash

# Clean and Build All Services
echo "🧹 Cleaning and Building All BeamerParts Services..."

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

# First, check if we have Maven Wrapper at root level
if [ -f "./mvnw" ]; then
    echo -e "${BLUE}🔧 Building all modules from root (including shared)...${NC}"
    ./mvnw clean install -DskipTests
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ All modules built successfully from root${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️ Root build failed, trying individual service builds...${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ No root Maven wrapper found, building services individually...${NC}"
fi

# Clean and build shared module first (fallback method)
echo -e "${BLUE}🔧 Building shared module...${NC}"
cd shared
if [ -f "./mvnw" ]; then
    ./mvnw clean install
else
    # Use Maven from PATH or use a service's mvnw
    if command -v mvn >/dev/null 2>&1; then
        mvn clean install
    else
        # Borrow mvnw from user-service
        "$PROJECT_ROOT/user-service/mvnw" -f pom.xml clean install
    fi
fi
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Shared module built successfully${NC}"
else
    echo -e "${RED}❌ Failed to build shared module${NC}"
    exit 1
fi

# List of services to build
services=("api-gateway" "user-service" "vehicle-service" "product-service" "order-service")

echo -e "\n${BLUE}🏗️ Building all services...${NC}"

for service in "${services[@]}"; do
    echo -e "\n${YELLOW}🔨 Building $service...${NC}"
    cd "$PROJECT_ROOT/$service"
    
    # Clean and compile
    ./mvnw clean compile
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $service built successfully${NC}"
    else
        echo -e "${RED}❌ Failed to build $service${NC}"
        exit 1
    fi
done

echo -e "\n${GREEN}🎉 All services built successfully!${NC}"
echo -e "${YELLOW}💡 You can now start the services using the development scripts${NC}"
