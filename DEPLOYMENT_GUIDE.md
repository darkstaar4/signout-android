# SignOut Android App - AWS Backend Deployment Guide

This guide walks you through deploying the AWS backend infrastructure and configuring the Android app to use real Cognito data instead of mock data.

## Overview

The AWS backend consists of:
- **Lambda Functions**: For user discovery and search
- **API Gateway**: REST API endpoints
- **Cognito Integration**: Real user data from your existing User Pool

## Prerequisites

### 1. AWS Setup
- AWS CLI installed and configured with appropriate permissions
- SAM CLI installed (`pip install aws-sam-cli`)
- Existing Cognito User Pool with custom attributes:
  - `custom:specialty`
  - `custom:office_city`
  - `custom:npi_number`

### 2. Development Environment
- Node.js 18.x or later
- Java 21 (for Android app)
- Android Studio

## Step 1: Deploy AWS Backend

### 1.1 Navigate to Backend Directory
```bash
cd aws-backend
```

### 1.2 Configure Your User Pool ID
You'll need your Cognito User Pool ID. You can find this in the AWS Console:
1. Go to AWS Cognito
2. Select your User Pool
3. Copy the User Pool ID (format: `us-west-2_XXXXXXXXX`)

### 1.3 Run Deployment Script
```bash
./deploy.sh
```

The script will:
1. Check prerequisites (AWS CLI, SAM CLI)
2. Prompt for your User Pool ID
3. Install Lambda dependencies
4. Build and deploy the SAM application
5. Output the API Gateway endpoints

### 1.4 Note the API Gateway URL
After successful deployment, you'll see output like:
```
API Gateway URL: https://abc123def4.execute-api.us-west-2.amazonaws.com/prod
User Discovery Endpoint: https://abc123def4.execute-api.us-west-2.amazonaws.com/prod/api/v1/users/cognito/discover
User Search Endpoint: https://abc123def4.execute-api.us-west-2.amazonaws.com/prod/api/v1/users/cognito/search
```

**Save this API Gateway URL - you'll need it for the Android app configuration.**

## Step 2: Test the Backend

### 2.1 Test User Discovery
```bash
curl "https://your-api-gateway-url/prod/api/v1/users/cognito/discover?matrix_user_id=@nbaig:signout.io"
```

Expected response:
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

### 2.2 Test User Search
```bash
curl "https://your-api-gateway-url/prod/api/v1/users/cognito/search?query=nbaig&limit=10"
```

Expected response:
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

## Step 3: Configure Android App

### 3.1 Update Backend Service URL
Edit the file:
```
libraries/usersearch/impl/src/main/kotlin/io/element/android/libraries/usersearch/impl/network/CognitoUserBackendService.kt
```

Replace the placeholder URL with your actual API Gateway URL:
```kotlin
// Before:
private val backendBaseUrl = "https://your-api-gateway-id.execute-api.us-west-2.amazonaws.com/prod/"

// After (use your actual API Gateway URL):
private val backendBaseUrl = "https://abc123def4.execute-api.us-west-2.amazonaws.com/prod/"
```

### 3.2 Build and Install Android App
```bash
cd ..  # Go back to project root
./gradlew assembleDebug
adb install -r app/build/outputs/apk/gplay/debug/app-gplay-universal-debug.apk
```

## Step 4: Test the Integration

### 4.1 Monitor Logs
Start monitoring Android logs:
```bash
adb logcat -c && adb logcat | grep -E "(CognitoUserBackendService|MatrixUserRepository|UserDirectoryService)" --line-buffered
```

### 4.2 Test User Search in App
1. Open the SignOut app
2. Navigate to the user search/invite screen
3. Search for "nbaig" or your username
4. Verify you see real Cognito data with enhanced format:
   - Name: "nbaig do"
   - Details: "@nbaig | Addiction Medicine | Fresno"

### 4.3 Expected Log Output
You should see logs like:
```
CognitoUserBackendService: Successfully searched users for query: nbaig, found 1 users
MatrixUserRepository: Found 1 users in backend directory for query: 'nbaig'
MatrixUserRepository: UserMapping specialty: Addiction Medicine, officeCity: Fresno
```

## Step 5: Verify Real Data Integration

### 5.1 Check Data Sources
The app should now be using:
- **Real Cognito data** for your current user (from `CognitoAuthService`)
- **Real backend API** for discovering other users (from AWS Lambda)
- **No mock/test data** - all hard-coded test users removed

### 5.2 Search Functionality
Test these search patterns:
- Search by first name: "nbaig"
- Search by last name: "do"
- Search by username: "@nbaig"
- Search by partial username: "nba"

All searches should return real Cognito users with enhanced format display.

## Troubleshooting

### Common Issues

#### 1. API Gateway URL Not Working
- Verify the URL is correct from deployment output
- Check CORS configuration in AWS Console
- Ensure Lambda functions are deployed successfully

#### 2. Cognito Permissions
- Verify Lambda execution role has proper Cognito permissions
- Check User Pool ID is correct
- Ensure custom attributes exist in your User Pool

#### 3. Android App Not Showing Data
- Verify backend URL is updated correctly
- Check network connectivity
- Monitor logs for API errors

#### 4. Search Returns Empty Results
- Verify users exist in your Cognito User Pool
- Check user attributes are populated
- Test API endpoints directly with curl

### Debug Steps

1. **Check CloudWatch Logs**:
   - Go to AWS CloudWatch
   - Check logs for Lambda functions
   - Look for error messages

2. **Test API Directly**:
   ```bash
   curl -v "https://your-api-gateway-url/prod/api/v1/users/cognito/discover?matrix_user_id=@nbaig:signout.io"
   ```

3. **Android Logs**:
   ```bash
   adb logcat | grep -i "cognito\|backend\|user"
   ```

## Configuration Files

After deployment, you'll have these configuration files:
- `aws-backend/android-config.properties` - Contains API URLs for reference
- CloudFormation stack outputs in AWS Console

## Security Considerations

1. **CORS**: Currently set to `*` for development. Update for production.
2. **API Gateway**: No authentication required. Consider adding API keys or Cognito authentication.
3. **Lambda Permissions**: Minimal IAM permissions for Cognito access.

## Next Steps

1. **Production Deployment**: Update CORS settings and add authentication
2. **Monitoring**: Set up CloudWatch alarms for Lambda errors
3. **Caching**: Enable API Gateway caching for better performance
4. **Testing**: Add integration tests for the backend API

## Support

If you encounter issues:
1. Check the logs (CloudWatch and Android)
2. Verify configuration matches this guide
3. Test API endpoints independently
4. Review AWS resource permissions

---

**Important**: Make sure to update the `backendBaseUrl` in the Android app with your actual API Gateway URL before building and testing! 