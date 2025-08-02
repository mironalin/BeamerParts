#!/bin/bash

# BeamerParts Development Environment Manager
# This script provides an interactive menu to manage the development environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Logo
echo -e "${BLUE}"
cat << "EOF"
╔══════════════════════════════════════════════╗
║           🚗 BeamerParts Development         ║
║              Environment Manager             ║
╚══════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Function to display menu
show_menu() {
    echo -e "${CYAN}Select an option:${NC}"
    echo -e "${GREEN}1)${NC} 🐳 Start Infrastructure (Docker)"
    echo -e "${GREEN}2)${NC} 🏗️  Start All Services (Local)"
    echo -e "${GREEN}3)${NC} 🌐 Start API Gateway Only"
    echo -e "${GREEN}4)${NC} 👤 Start User Service Only"
    echo -e "${GREEN}5)${NC} 🚗 Start Vehicle Service Only"
    echo -e "${GREEN}6)${NC} 📦 Start Product Service Only"
    echo -e "${GREEN}7)${NC} 📋 Start Order Service Only"
    echo -e "${GREEN}8)${NC} 🔄 Full Development Setup (Infrastructure + Services)"
    echo -e "${GREEN}9)${NC} 🔄 Rebuild & Restart All Services (Keep Infrastructure)"
    echo -e "${GREEN}10)${NC} ⚡ Quick Restart Single Service"
    echo -e "${GREEN}11)${NC} 🧪 Run Tests"
    echo -e "${GREEN}12)${NC} 🧹 Clean & Build All"
    echo -e "${GREEN}13)${NC} 📊 Show Service Status"
    echo -e "${GREEN}14)${NC} 🛑 Stop All Services"
    echo -e "${GREEN}15)${NC} 📝 View Logs"
    echo -e "${GREEN}16)${NC} 🧪 Test API Gateway (Phase 1 Endpoints)"
    echo -e "${RED}0)${NC} Exit"
    echo ""
}

# Function to start infrastructure
start_infrastructure() {
    echo -e "${BLUE}🐳 Starting Docker Infrastructure...${NC}"
    "$SCRIPT_DIR/docker/start-infra.sh"
}

# Function to start all services
start_all_services() {
    echo -e "${BLUE}🏗️ Starting All Services...${NC}"
    "$SCRIPT_DIR/dev/start-all-services.sh"
}

# Function for full setup
full_setup() {
    echo -e "${BLUE}🔄 Running Full Development Setup...${NC}"
    start_infrastructure
    echo -e "${YELLOW}Waiting 10 seconds for infrastructure to stabilize...${NC}"
    sleep 10
    start_all_services
}

# Function to rebuild and restart services
rebuild_restart_services() {
    echo -e "${BLUE}🔄 Rebuilding and Restarting Services...${NC}"
    "$SCRIPT_DIR/dev/rebuild-restart-services.sh"
}

# Function for quick restart single service
quick_restart_service() {
    echo -e "${BLUE}⚡ Quick Restart Single Service...${NC}"
    "$SCRIPT_DIR/dev/quick-restart-service.sh"
}

# Function to show service status
show_status() {
    echo -e "${BLUE}📊 Service Status:${NC}"
    "$SCRIPT_DIR/dev/check-status.sh"
}

# Function to stop all services
stop_all() {
    echo -e "${RED}🛑 Stopping All Services...${NC}"
    "$SCRIPT_DIR/dev/stop-all.sh"
}

# Function to view logs
view_logs() {
    echo -e "${BLUE}📝 Available log options:${NC}"
    echo "1) Infrastructure logs"
    echo "2) Service logs"
    echo "3) All logs"
    read -p "Select option: " log_choice
    
    case $log_choice in
        1) "$SCRIPT_DIR/dev/show-infra-logs.sh" ;;
        2) "$SCRIPT_DIR/dev/show-service-logs.sh" ;;
        3) "$SCRIPT_DIR/dev/show-all-logs.sh" ;;
        *) echo -e "${RED}Invalid option${NC}" ;;
    esac
}

# Main menu loop
while true; do
    show_menu
    read -p "Enter your choice [0-16]: " choice
    echo ""
    
    case $choice in
        1) start_infrastructure ;;
        2) start_all_services ;;
        3) "$SCRIPT_DIR/services/start-api-gateway.sh" ;;
        4) "$SCRIPT_DIR/services/start-user-service.sh" ;;
        5) "$SCRIPT_DIR/services/start-vehicle-service.sh" ;;
        6) "$SCRIPT_DIR/services/start-product-service.sh" ;;
        7) "$SCRIPT_DIR/services/start-order-service.sh" ;;
        8) full_setup ;;
        9) rebuild_restart_services ;;
        10) quick_restart_service ;;
        11) "$SCRIPT_DIR/dev/run-tests.sh" ;;
        12) "$SCRIPT_DIR/dev/clean-build.sh" ;;
        13) show_status ;;
        14) stop_all ;;
        15) view_logs ;;
        16) "$SCRIPT_DIR/dev/test-api-gateway.sh" ;;
        0) echo -e "${GREEN}👋 Goodbye!${NC}"; exit 0 ;;
        *) echo -e "${RED}Invalid option. Please try again.${NC}" ;;
    esac
    
    echo ""
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read
    clear
done
