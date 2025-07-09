const AWS = require('aws-sdk');
const cognito = new AWS.CognitoIdentityServiceProvider();

// Environment variables
const USER_POOL_ID = process.env.USER_POOL_ID;
const CORS_ORIGIN = process.env.CORS_ORIGIN || '*';

// CORS headers
const corsHeaders = {
    'Access-Control-Allow-Origin': CORS_ORIGIN,
    'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
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
        const { matrix_user_id } = event.queryStringParameters || {};
        
        if (!matrix_user_id) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({ 
                    error: 'matrix_user_id parameter is required' 
                })
            };
        }
        
        console.log('Searching for user with Matrix ID:', matrix_user_id);
        
        // Extract username from Matrix ID (e.g., @nbaig:signout.io -> nbaig)
        const username = matrix_user_id.replace(/^@/, '').split(':')[0];
        
        // Search for user in Cognito User Pool
        const user = await findUserByUsername(username);
        
        if (!user) {
            return {
                statusCode: 404,
                headers: corsHeaders,
                body: JSON.stringify({ 
                    error: 'User not found' 
                })
            };
        }
        
        // Convert Cognito user to our format
        const cognitoUser = convertCognitoUser(user, matrix_user_id);
        
        return {
            statusCode: 200,
            headers: corsHeaders,
            body: JSON.stringify(cognitoUser)
        };
        
    } catch (error) {
        console.error('Error:', error);
        return {
            statusCode: 500,
            headers: corsHeaders,
            body: JSON.stringify({ 
                error: 'Internal server error',
                details: error.message 
            })
        };
    }
};

async function findUserByUsername(username) {
    try {
        const params = {
            UserPoolId: USER_POOL_ID,
            Username: username
        };
        
        const result = await cognito.adminGetUser(params).promise();
        return result;
    } catch (error) {
        if (error.code === 'UserNotFoundException') {
            return null;
        }
        throw error;
    }
}

function convertCognitoUser(cognitoUser, matrixUserId) {
    const attributes = {};
    
    // Convert Cognito attributes to key-value pairs
    if (cognitoUser.UserAttributes) {
        cognitoUser.UserAttributes.forEach(attr => {
            attributes[attr.Name] = attr.Value;
        });
    }
    
    // Extract Matrix username from Matrix ID
    const matrixUsername = matrixUserId.replace(/^@/, '').split(':')[0];
    
    return {
        matrix_user_id: matrixUserId,
        matrix_username: matrixUsername,
        cognito_username: cognitoUser.Username,
        given_name: attributes.given_name || '',
        family_name: attributes.family_name || '',
        display_name: `${attributes.given_name || ''} ${attributes.family_name || ''}`.trim(),
        email: attributes.email || '',
        specialty: attributes['custom:specialty'] || null,
        office_city: attributes['custom:office_city'] || null,
        npi_number: attributes['custom:npi_number'] || null,
        phone_number: attributes.phone_number || null,
        avatar_url: attributes.picture || null,
        created_at: cognitoUser.UserCreateDate?.toISOString(),
        updated_at: cognitoUser.UserLastModifiedDate?.toISOString()
    };
} 