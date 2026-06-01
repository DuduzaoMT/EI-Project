#!/bin/bash

# Kong Gateway Setup Script
# This script registers all microservices and routes in Kong API Gateway

# Receive parameters
addressKong=$1
addressTelemetry=$2
addressFlexibilityEvent=$3
addressGridBalancing=$4
addressEnergyAnalytics=$5
addressArtificialIntelligence=$6
addressProsumer=$7
addressUtilityOperator=$8
addressAssetLink=$9

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting Kong Gateway configuration...${NC}"

# Function to wait for Kong to be available
wait_for_kong() {
    local max_attempts=60
    local attempt=1
    local kong_url="http://$addressKong:8001"
    
    echo -e "${YELLOW}Waiting for Kong Admin API to be available at $kong_url...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$kong_url/status" > /dev/null 2>&1; then
            echo -e "${GREEN}Kong is now available!${NC}"
            return 0
        fi
        
        echo "Attempt $attempt/$max_attempts: Kong not ready yet, waiting..."
        sleep 10
        ((attempt++))
    done
    
    echo -e "${RED}Error: Kong Admin API is not available after ${max_attempts} attempts${NC}"
    return 1
}

# Function to register a service and its route
register_service() {
    local service_name=$1
    local service_url=$2
    local route_path=$3
    local route_name=$4
    
    echo -e "${YELLOW}Registering service: $service_name${NC}"
    
    # Register the service
    curl -s -X POST "http://$addressKong:8001/services/" \
        --data "name=$service_name" \
        --data "url=$service_url" \
        -H "Content-Type: application/x-www-form-urlencoded" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Service '$service_name' registered${NC}"
    else
        echo -e "${RED}✗ Failed to register service '$service_name'${NC}"
        return 1
    fi
    
    # Register the route
    curl -s -X POST "http://$addressKong:8001/services/$service_name/routes" \
        --data "name=$route_name" \
        --data "paths=$route_path" \
        -H "Content-Type: application/x-www-form-urlencoded" > /dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Route '$route_name' ($route_path) registered for service '$service_name'${NC}"
    else
        echo -e "${RED}✗ Failed to register route for service '$service_name'${NC}"
        return 1
    fi
    
    echo ""
}

# Main execution
# # main() {
    # Validate parameters
    if [ -z "$addressKong" ]; then
        echo -e "${RED}Error: Kong address not provided${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Kong Admin API Address: $addressKong${NC}"
    echo ""
    
    # Wait for Kong to be available
    wait_for_kong
    if [ $? -ne 0 ]; then
        exit 1
    fi
    
    echo -e "${YELLOW}Registering all microservices...${NC}"
    echo ""
    
    # Register all services and routes
    # Each microservice follows the pattern: service_name, service_url, route_path, route_name
    
    register_service "telemetry-service" "http://$addressTelemetry:8080" "/telemetry" "telemetry-route"
    register_service "flexibility-event-service" "http://$addressFlexibilityEvent:8080" "/flexibility-event" "flexibility-event-route"
    register_service "grid-balancing-service" "http://$addressGridBalancing:8080" "/grid-balancing" "grid-balancing-route"
    register_service "energy-analytics-service" "http://$addressEnergyAnalytics:8080" "/energy-analytics" "energy-analytics-route"
    register_service "artificial-intelligence-service" "http://$addressArtificialIntelligence:8080" "/artificial-intelligence" "artificial-intelligence-route"
    register_service "prosumer-service" "http://$addressProsumer:8080" "/prosumer" "prosumer-route"
    register_service "utility-operator-service" "http://$addressUtilityOperator:8080" "/utility-operator" "utility-operator-route"
    register_service "assetlink-service" "http://$addressAssetLink:8080" "/assetlink" "assetlink-route"
    
    echo -e "${GREEN}Kong Gateway configuration completed!${NC}"
    echo ""
    echo -e "${YELLOW}Kong Admin API: http://$addressKong:8001${NC}"
    echo -e "${YELLOW}Kong Gateway API: http://$addressKong:8000${NC}"
    echo ""
    echo "Registered services:"
    curl -s "http://$addressKong:8001/services/" | grep -o '"name":"[^"]*"' | cut -d'"' -f4
}

# Execute main function
main
