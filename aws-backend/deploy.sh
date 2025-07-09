#!/bin/bash

# SignOut Cognito User Discovery API Deployment Script
# This script deploys the Lambda functions and API Gateway using AWS SAM

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
STACK_NAME="signout-cognito-user-api"
REGION="us-west-2"
S3_BUCKET="signout-sam-deployment-artifacts"  # Replace with your S3 bucket
USER_POOL_ID=""  # Will be prompted
CORS_ORIGIN="*"
MATRIX_DOMAIN="signout.io"
STAGE="prod"

echo -e "${GREEN}SignOut Cognito User Discovery API Deployment${NC}"
echo "=============================================="

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Check if SAM CLI is installed
if ! command -v sam &> /dev/null; then
    echo -e "${RED}SAM CLI is not installed. Please install it first.${NC}"
    echo "Install with: pip install aws-sam-cli"
    exit 1
fi

# Get AWS account info
echo -e "${YELLOW}Checking AWS credentials...${NC}"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region || echo $REGION)
echo "AWS Account ID: $AWS_ACCOUNT_ID"
echo "AWS Region: $AWS_REGION"

# Prompt for User Pool ID if not set
if [ -z "$USER_POOL_ID" ]; then
    echo -e "${YELLOW}Please enter your Cognito User Pool ID:${NC}"
    read -p "User Pool ID: " USER_POOL_ID
fi

if [ -z "$USER_POOL_ID" ]; then
    echo -e "${RED}User Pool ID is required${NC}"
    exit 1
fi

# Create S3 bucket for SAM artifacts if it doesn't exist
echo -e "${YELLOW}Checking S3 bucket for SAM artifacts...${NC}"
if ! aws s3 ls "s3://$S3_BUCKET" 2>/dev/null; then
    echo "Creating S3 bucket: $S3_BUCKET"
    aws s3 mb "s3://$S3_BUCKET" --region $AWS_REGION
fi

# Install Lambda dependencies
echo -e "${YELLOW}Installing Lambda dependencies...${NC}"
cd lambda/user-discovery
npm install --production
cd ../user-search
npm install --production
cd ../..

# Build the SAM application
echo -e "${YELLOW}Building SAM application...${NC}"
sam build

# Deploy the SAM application
echo -e "${YELLOW}Deploying SAM application...${NC}"
sam deploy \
    --stack-name $STACK_NAME \
    --s3-bucket $S3_BUCKET \
    --region $AWS_REGION \
    --capabilities CAPABILITY_IAM \
    --parameter-overrides \
        UserPoolId=$USER_POOL_ID \
        CorsOrigin=$CORS_ORIGIN \
        MatrixDomain=$MATRIX_DOMAIN \
        Stage=$STAGE \
    --confirm-changeset

# Get the API Gateway URL
echo -e "${YELLOW}Getting API Gateway URL...${NC}"
API_URL=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
    --output text)

USER_DISCOVERY_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`UserDiscoveryEndpoint`].OutputValue' \
    --output text)

USER_SEARCH_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`UserSearchEndpoint`].OutputValue' \
    --output text)

echo -e "${GREEN}Deployment completed successfully!${NC}"
echo "=============================================="
echo "API Gateway URL: $API_URL"
echo "User Discovery Endpoint: $USER_DISCOVERY_ENDPOINT"
echo "User Search Endpoint: $USER_SEARCH_ENDPOINT"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update your Android app configuration with the API Gateway URL"
echo "2. Test the endpoints with your Cognito User Pool data"
echo ""
echo -e "${YELLOW}Test commands:${NC}"
echo "# Test user discovery:"
echo "curl \"$USER_DISCOVERY_ENDPOINT?matrix_user_id=@nbaig:signout.io\""
echo ""
echo "# Test user search:"
echo "curl \"$USER_SEARCH_ENDPOINT?query=nbaig&limit=10\""
echo ""

# Save configuration for Android app
echo -e "${YELLOW}Saving configuration for Android app...${NC}"
cat > android-config.properties << EOF
# SignOut Cognito User Discovery API Configuration
# Generated on $(date)

# API Gateway Base URL
API_BASE_URL=$API_URL

# Specific Endpoints
USER_DISCOVERY_ENDPOINT=$USER_DISCOVERY_ENDPOINT
USER_SEARCH_ENDPOINT=$USER_SEARCH_ENDPOINT

# Configuration
USER_POOL_ID=$USER_POOL_ID
MATRIX_DOMAIN=$MATRIX_DOMAIN
CORS_ORIGIN=$CORS_ORIGIN
STAGE=$STAGE
EOF

echo -e "${GREEN}Configuration saved to android-config.properties${NC}"
echo "Use this file to configure your Android app." 