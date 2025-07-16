const AWS = require('aws-sdk');

// Initialize AWS services
const cognito = new AWS.CognitoIdentityServiceProvider({
    region: process.env.AWS_REGION || 'us-east-1'
});

const USER_POOL_ID = process.env.USER_POOL_ID || 'us-east-1_ltpOFMyVw';

exports.handler = async (event) => {
    console.log('User deactivation request:', JSON.stringify(event, null, 2));
    
    // Set CORS headers
    const headers = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        'Content-Type': 'application/json'
    };
    
    // Handle preflight OPTIONS request
    if (event.httpMethod === 'OPTIONS') {
        return {
            statusCode: 200,
            headers,
            body: JSON.stringify({ message: 'CORS preflight successful' })
        };
    }
    
    try {
        // Parse request body
        let requestBody;
        try {
            requestBody = JSON.parse(event.body || '{}');
        } catch (parseError) {
            console.error('Invalid JSON in request body:', parseError);
            return {
                statusCode: 400,
                headers,
                body: JSON.stringify({
                    success: false,
                    error: 'Invalid JSON in request body'
                })
            };
        }
        
        const { username, action } = requestBody;
        
        // Validate required fields
        if (!username) {
            return {
                statusCode: 400,
                headers,
                body: JSON.stringify({
                    success: false,
                    error: 'Username is required'
                })
            };
        }
        
        // Validate action (default to 'disable' if not provided)
        const validActions = ['disable', 'enable', 'delete'];
        const userAction = action || 'disable';
        
        if (!validActions.includes(userAction)) {
            return {
                statusCode: 400,
                headers,
                body: JSON.stringify({
                    success: false,
                    error: `Invalid action. Must be one of: ${validActions.join(', ')}`
                })
            };
        }
        
        console.log(`Attempting to ${userAction} user: ${username}`);
        
        // First, check if user exists
        try {
            const getUserParams = {
                UserPoolId: USER_POOL_ID,
                Username: username
            };
            
            const userResult = await cognito.adminGetUser(getUserParams).promise();
            console.log('User found:', userResult.Username);
            
            // Check current user status
            const currentStatus = userResult.UserStatus;
            console.log('Current user status:', currentStatus);
            
        } catch (getUserError) {
            console.error('Error getting user:', getUserError);
            
            if (getUserError.code === 'UserNotFoundException') {
                return {
                    statusCode: 404,
                    headers,
                    body: JSON.stringify({
                        success: false,
                        error: 'User not found'
                    })
                };
            }
            
            throw getUserError;
        }
        
        // Perform the requested action
        let result;
        
        switch (userAction) {
            case 'disable':
                const disableParams = {
                    UserPoolId: USER_POOL_ID,
                    Username: username
                };
                
                result = await cognito.adminDisableUser(disableParams).promise();
                console.log('User disabled successfully:', result);
                break;
                
            case 'enable':
                const enableParams = {
                    UserPoolId: USER_POOL_ID,
                    Username: username
                };
                
                result = await cognito.adminEnableUser(enableParams).promise();
                console.log('User enabled successfully:', result);
                break;
                
            case 'delete':
                const deleteParams = {
                    UserPoolId: USER_POOL_ID,
                    Username: username
                };
                
                result = await cognito.adminDeleteUser(deleteParams).promise();
                console.log('User deleted successfully:', result);
                break;
        }
        
        // Return success response
        return {
            statusCode: 200,
            headers,
            body: JSON.stringify({
                success: true,
                message: `User ${username} ${userAction}d successfully`,
                username: username,
                action: userAction,
                timestamp: new Date().toISOString()
            })
        };
        
    } catch (error) {
        console.error('Error in user deactivation:', error);
        
        // Handle specific AWS errors
        let errorMessage = 'Internal server error';
        let statusCode = 500;
        
        if (error.code === 'UserNotFoundException') {
            errorMessage = 'User not found';
            statusCode = 404;
        } else if (error.code === 'InvalidParameterException') {
            errorMessage = 'Invalid parameters provided';
            statusCode = 400;
        } else if (error.code === 'NotAuthorizedException') {
            errorMessage = 'Not authorized to perform this action';
            statusCode = 403;
        } else if (error.code === 'TooManyRequestsException') {
            errorMessage = 'Too many requests. Please try again later.';
            statusCode = 429;
        }
        
        return {
            statusCode,
            headers,
            body: JSON.stringify({
                success: false,
                error: errorMessage,
                details: error.message
            })
        };
    }
}; 