# SignOut Cognito User Discovery API

This AWS backend provides REST API endpoints for discovering and searching Cognito users for the SignOut Android application.

## Architecture

- **AWS Lambda**: Serverless functions for user discovery and search
- **API Gateway**: REST API endpoints with CORS support
- **Amazon Cognito**: User Pool integration for user data
- **CloudWatch**: Logging and monitoring

## Prerequisites

1. **AWS CLI** installed and configured
2. **SAM CLI** installed (`pip install aws-sam-cli`)
3. **Node.js 18.x** or later
4. **Existing Cognito User Pool** with custom attributes:
   - `custom:specialty`
   - `custom:office_city`
   - `custom:npi_number`

## Quick Start

### 1. Clone and Setup

```bash
git clone <repository>
cd aws-backend
```

### 2. Configure Your User Pool ID

Edit `deploy.sh` and update the `USER_POOL_ID` variable, or you'll be prompted during deployment.

### 3. Deploy

```bash
chmod +x deploy.sh
./deploy.sh
```

The script will:
- Install Lambda dependencies
- Build the SAM application
- Deploy to AWS
- Output the API Gateway endpoints

### 4. Test the API

After deployment, test the endpoints:

```bash
# Test user discovery
curl "https://your-api-gateway-url/prod/api/v1/users/cognito/discover?matrix_user_id=@nbaig:signout.io"

# Test user search
curl "https://your-api-gateway-url/prod/api/v1/users/cognito/search?query=nbaig&limit=10"
```

## API Endpoints

### User Discovery

**GET** `/api/v1/users/cognito/discover`

Discovers a specific user by Matrix ID.

**Parameters:**
- `matrix_user_id` (required): Matrix user ID (e.g., `@nbaig:signout.io`)

**Response:**
```json
{
  "matrix_user_id": "@nbaig:signout.io",
  "matrix_username": "nbaig",
  "cognito_username": "nbaig",
  "given_name": "nbaig",
  "family_name": "do",
  "display_name": "nbaig do",
  "email": "nbaig@signout.io",
  "specialty": "Addiction Medicine",
  "office_city": "Fresno",
  "npi_number": null,
  "phone_number": null,
  "avatar_url": null,
  "created_at": "2024-01-01T00:00:00.000Z",
  "updated_at": "2024-01-01T00:00:00.000Z"
}
```

### User Search

**GET** `/api/v1/users/cognito/search`

Searches for users by query string.

**Parameters:**
- `query` (required): Search query
- `limit` (optional): Maximum number of results (default: 10, max: 60)

**Response:**
```json
{
  "users": [
    {
      "matrix_user_id": "@nbaig:signout.io",
      "matrix_username": "nbaig",
      "cognito_username": "nbaig",
      "given_name": "nbaig",
      "family_name": "do",
      "display_name": "nbaig do",
      "email": "nbaig@signout.io",
      "specialty": "Addiction Medicine",
      "office_city": "Fresno",
      "npi_number": null,
      "phone_number": null,
      "avatar_url": null,
      "created_at": "2024-01-01T00:00:00.000Z",
      "updated_at": "2024-01-01T00:00:00.000Z"
    }
  ],
  "total": 1,
  "query": "nbaig",
  "limit": 10
}
```

## Configuration

### Environment Variables

The Lambda functions use these environment variables:

- `USER_POOL_ID`: Cognito User Pool ID
- `CORS_ORIGIN`: CORS origin (default: '*')
- `MATRIX_DOMAIN`: Matrix domain (default: 'signout.io')

### SAM Parameters

Configure these parameters in `template.yaml` or pass them during deployment:

- `UserPoolId`: Your Cognito User Pool ID
- `CorsOrigin`: CORS origin for API Gateway
- `MatrixDomain`: Matrix domain for user IDs
- `Stage`: API Gateway stage (default: 'prod')

## Android App Integration

After deployment, update your Android app configuration:

1. Copy the API Gateway URL from the deployment output
2. Update the `CognitoUserBackendService` base URL
3. Rebuild and deploy your Android app

Example configuration in Android:

```kotlin
// In CognitoUserBackendService.kt
private val baseUrl = "https://your-api-gateway-url.execute-api.us-west-2.amazonaws.com/prod/"
```

## Security

### IAM Permissions

The Lambda functions have minimal IAM permissions:
- `cognito-idp:AdminGetUser`
- `cognito-idp:ListUsers`

### CORS

CORS is configured to allow requests from your Android app. Update `CorsOrigin` parameter to restrict access.

## Monitoring

### CloudWatch Logs

Lambda function logs are available in CloudWatch:
- `/aws/lambda/signout-cognito-user-api-user-discovery`
- `/aws/lambda/signout-cognito-user-api-user-search`

### Metrics

Monitor API Gateway and Lambda metrics in CloudWatch:
- Request count
- Error rate
- Latency
- Lambda duration and errors

## Troubleshooting

### Common Issues

1. **User Pool ID not found**
   - Verify your User Pool ID is correct
   - Check the AWS region matches your User Pool

2. **CORS errors**
   - Update the `CorsOrigin` parameter
   - Ensure your Android app uses the correct API Gateway URL

3. **Permission errors**
   - Verify Lambda execution role has Cognito permissions
   - Check User Pool ARN in IAM policy

### Debugging

Enable debug logging in Lambda functions by adding:
```javascript
console.log('Debug info:', JSON.stringify(data, null, 2));
```

## Development

### Local Testing

Use SAM CLI for local development:

```bash
# Start local API Gateway
sam local start-api

# Test locally
curl "http://localhost:3000/api/v1/users/cognito/discover?matrix_user_id=@nbaig:signout.io"
```

### Manual Deployment

For manual deployment without the script:

```bash
# Build
sam build

# Deploy
sam deploy --guided
```

## Cost Optimization

- Lambda functions use ARM64 architecture for better price/performance
- CloudWatch log retention is set to 14 days
- API Gateway caching can be enabled for frequently accessed endpoints

## Support

For issues or questions:
1. Check CloudWatch logs for error details
2. Verify Cognito User Pool configuration
3. Test API endpoints directly with curl
4. Review IAM permissions 