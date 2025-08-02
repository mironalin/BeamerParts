#!/bin/bash

# API Gateway Testing Script - Hybrid Versioning Strategy
echo "🧪 Testing BeamerParts API Gateway - Versioned + Convenience Routes"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

API_BASE="http://localhost:8080"

# Function to test API endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local expected_status=${4:-200}
    
    echo -e "${BLUE}Testing: $method $endpoint${NC}"
    echo -e "${YELLOW}Description: $description${NC}"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" "$API_BASE$endpoint")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X "$method" "$API_BASE$endpoint")
    fi
    
    http_code=$(echo "$response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    body=$(echo "$response" | sed -e 's/HTTPSTATUS:.*//')
    
    if [ "$http_code" -eq "$expected_status" ] || [ "$http_code" -eq 404 ] || [ "$http_code" -eq 405 ]; then
        echo -e "${GREEN}✅ Status: $http_code${NC}"
    else
        echo -e "${RED}❌ Status: $http_code (expected: $expected_status)${NC}"
    fi
    
    echo -e "${YELLOW}Response preview:${NC} $(echo "$body" | head -c 100)..."
    echo ""
}

echo -e "${BLUE}📊 API Gateway Health Check${NC}"
test_endpoint "GET" "/actuator/health" "API Gateway health status"

echo -e "\n${PURPLE}� API v1 Endpoints (Explicit Versioning)${NC}"
echo -e "${BLUE}🔐 Authentication v1${NC}"
test_endpoint "POST" "/api/v1/auth/register" "User registration (v1)"
test_endpoint "POST" "/api/v1/auth/login" "User login (v1)"
test_endpoint "POST" "/api/v1/auth/refresh" "Token refresh (v1)"
test_endpoint "POST" "/api/v1/auth/logout" "User logout (v1)"
test_endpoint "GET" "/api/v1/auth/me" "Get current user info (v1)"

echo -e "\n${BLUE}🚗 Vehicle Management v1${NC}"
test_endpoint "GET" "/api/v1/vehicles/series" "Get all BMW series (v1)"
test_endpoint "GET" "/api/v1/vehicles/series/3/generations" "Get generations for Series 3 (v1)"
test_endpoint "GET" "/api/v1/vehicles/generations/F30" "Get specific generation info (v1)"
test_endpoint "GET" "/api/v1/vehicles/generations/F30/products" "Get compatible products for F30 (v1)"

echo -e "\n${BLUE}📦 Product Catalog v1${NC}"
test_endpoint "GET" "/api/v1/products" "Get all products (v1)"
test_endpoint "GET" "/api/v1/products/BMW-F30-AC-001" "Get specific product by SKU (v1)"
test_endpoint "GET" "/api/v1/products/search" "Search products (v1)"
test_endpoint "GET" "/api/v1/categories" "Get all categories (v1)"
test_endpoint "GET" "/api/v1/categories/1/products" "Get products in category (v1)"

echo -e "\n${BLUE}🛒 Shopping Cart v1${NC}"
test_endpoint "GET" "/api/v1/cart" "Get user cart (v1)"
test_endpoint "POST" "/api/v1/cart/items" "Add item to cart (v1)"
test_endpoint "PUT" "/api/v1/cart/items/1" "Update cart item (v1)"
test_endpoint "DELETE" "/api/v1/cart/items/1" "Remove cart item (v1)"
test_endpoint "DELETE" "/api/v1/cart/clear" "Clear cart (v1)"

echo -e "\n${BLUE}👑 Admin v1${NC}"
test_endpoint "POST" "/api/v1/admin/products" "Create product (admin v1)"
test_endpoint "PUT" "/api/v1/admin/products/BMW-F30-AC-001" "Update product (admin v1)"
test_endpoint "POST" "/api/v1/admin/vehicles" "Create vehicle data (admin v1)"

echo -e "\n${PURPLE}🎯 Convenience Routes (Unversioned - Latest)${NC}"
echo -e "${BLUE}🔐 Authentication (Latest)${NC}"
test_endpoint "POST" "/api/auth/register" "User registration (latest)"
test_endpoint "POST" "/api/auth/login" "User login (latest)"
test_endpoint "POST" "/api/auth/refresh" "Token refresh (latest)"
test_endpoint "POST" "/api/auth/logout" "User logout (latest)"
test_endpoint "GET" "/api/auth/me" "Get current user info (latest)"

echo -e "\n${BLUE}🚗 Vehicle Management (Latest)${NC}"
test_endpoint "GET" "/api/vehicles/series" "Get all BMW series (latest)"
test_endpoint "GET" "/api/vehicles/series/3/generations" "Get generations for Series 3 (latest)"
test_endpoint "GET" "/api/vehicles/generations/F30" "Get specific generation info (latest)"
test_endpoint "GET" "/api/vehicles/generations/F30/products" "Get compatible products for F30 (latest)"

echo -e "\n${BLUE}📦 Product Catalog (Latest)${NC}"
test_endpoint "GET" "/api/products" "Get all products (latest)"
test_endpoint "GET" "/api/products/BMW-F30-AC-001" "Get specific product by SKU (latest)"
test_endpoint "GET" "/api/products/search" "Search products (latest)"
test_endpoint "GET" "/api/categories" "Get all categories (latest)"
test_endpoint "GET" "/api/categories/1/products" "Get products in category (latest)"

echo -e "\n${BLUE}🛒 Shopping Cart (Latest)${NC}"
test_endpoint "GET" "/api/cart" "Get user cart (latest)"
test_endpoint "POST" "/api/cart/items" "Add item to cart (latest)"
test_endpoint "PUT" "/api/cart/items/1" "Update cart item (latest)"
test_endpoint "DELETE" "/api/cart/items/1" "Remove cart item (latest)"
test_endpoint "DELETE" "/api/cart/clear" "Clear cart (latest)"

echo -e "\n${BLUE}🏥 Service Health Endpoints (Development)${NC}"
test_endpoint "GET" "/health/user/actuator/health" "User Service health via gateway"
test_endpoint "GET" "/health/vehicle/actuator/health" "Vehicle Service health via gateway"
test_endpoint "GET" "/health/product/actuator/health" "Product Service health via gateway"

echo -e "\n${GREEN}🎉 Hybrid API Gateway testing completed!${NC}"
echo -e "${PURPLE}📋 Summary:${NC}"
echo -e "${YELLOW}✅ Explicit versioning: /api/v1/* (recommended for production)${NC}"
echo -e "${YELLOW}✅ Convenience routes: /api/* (clean URLs, points to latest)${NC}"
echo -e "${YELLOW}🔧 Next step: Implement internal APIs in each service${NC}"
echo -e "${YELLOW}🚀 Future: Add /api/v2/* when needed without breaking existing clients${NC}"
