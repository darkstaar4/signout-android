const AWS = require('aws-sdk');
const cognito = new AWS.CognitoIdentityServiceProvider();

// Environment variables
const USER_POOL_ID = process.env.USER_POOL_ID;
const CORS_ORIGIN = process.env.CORS_ORIGIN || '*';

// CORS headers
const corsHeaders = {
    'Access-Control-Allow-Origin': CORS_ORIGIN,
    'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token',
    'Access-Control-Allow-Methods': 'POST,OPTIONS',
    'Content-Type': 'application/json'
};

exports.handler = async (event) => {
    console.log('Event:', JSON.stringify(event, null, 2));
    
    // Handle CORS preflight
    if (event.httpMethod === 'OPTIONS') {
        return {
            statusCode: 200,
            headers: corsHeaders,
            body: JSON.stringify({ message: 'CORS preflight successful' })
        };
    }
    
    try {
        // Parse request body
        const body = JSON.parse(event.body || '{}');
        const { username, approval_status } = body;
        
        // Validate input
        if (!username) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({
                    success: false,
                    error: 'username is required'
                })
            };
        }
        
        if (!approval_status) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({
                    success: false,
                    error: 'approval_status is required'
                })
            };
        }
        
        // Validate approval_status values
        const validStatuses = ['pending', 'cleared'];
        if (!validStatuses.includes(approval_status)) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({
                    success: false,
                    error: `approval_status must be one of: ${validStatuses.join(', ')}`
                })
            };
        }
        
        console.log(`Updating approval status for user: ${username} to: ${approval_status}`);
        
        // First, check if user exists
        try {
            const getUserParams = {
                UserPoolId: USER_POOL_ID,
                Username: username
            };
            
            const userResult = await cognito.adminGetUser(getUserParams).promise();
            console.log('User found:', userResult.Username);
            
        } catch (getUserError) {
            console.error('Error getting user:', getUserError);
            
            if (getUserError.code === 'UserNotFoundException') {
                return {
                    statusCode: 404,
                    headers: corsHeaders,
                    body: JSON.stringify({
                        success: false,
                        error: 'User not found'
                    })
                };
            }
            
            throw getUserError;
        }
        
        // Update the approval status attribute
        const updateParams = {
            UserPoolId: USER_POOL_ID,
            Username: username,
            UserAttributes: [
                {
                    Name: 'custom:approval_status',
                    Value: approval_status
                }
            ]
        };
        
        const result = await cognito.adminUpdateUserAttributes(updateParams).promise();
        console.log('Approval status updated successfully:', result);
        
        // Return success response
        return {
            statusCode: 200,
            headers: corsHeaders,
            body: JSON.stringify({
                success: true,
                message: `User ${username} approval status updated to ${approval_status} successfully`,
                username: username,
                approval_status: approval_status,
                timestamp: new Date().toISOString()
            })
        };
        
    } catch (error) {
        console.error('Error updating approval status:', error);
        
        return {
            statusCode: 500,
            headers: corsHeaders,
            body: JSON.stringify({
                success: false,
                error: 'Internal server error',
                details: error.message
            })
        };
    }
}; 