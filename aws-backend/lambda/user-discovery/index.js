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
        
        // Search for user in Cognito User Pool by preferred_username
        const user = await findUserByPreferredUsername(username);
        
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

async function findUserByPreferredUsername(username) {
    try {
        // First try to find user by preferred_username using listUsers with filter
        const filterParams = {
            UserPoolId: USER_POOL_ID,
            Filter: `preferred_username = "${username}"`,
            Limit: 1
        };
        
        console.log('Searching by preferred_username filter:', filterParams.Filter);
        const filterResult = await cognito.listUsers(filterParams).promise();
        
        if (filterResult.Users && filterResult.Users.length > 0) {
            const foundUser = filterResult.Users[0];
            console.log('Found user by preferred_username filter:', foundUser.Username);
            
            // Get full user details with all attributes using adminGetUser
            try {
                const fullUserParams = {
                    UserPoolId: USER_POOL_ID,
                    Username: foundUser.Username
                };
                
                console.log('Getting full user details for:', foundUser.Username);
                const fullUserResult = await cognito.adminGetUser(fullUserParams).promise();
                console.log('Retrieved full user details with', fullUserResult.UserAttributes?.length || 0, 'attributes');
                return fullUserResult;
            } catch (fullUserError) {
                console.log('Failed to get full user details, using listUsers result:', fullUserError.message);
                return foundUser;
            }
        }
        
        // If filter search fails, try adminGetUser with the username directly
        // (in case the username in Cognito matches the preferred_username)
        try {
            const directParams = {
                UserPoolId: USER_POOL_ID,
                Username: username
            };
            
            console.log('Trying direct adminGetUser with username:', username);
            const directResult = await cognito.adminGetUser(directParams).promise();
            console.log('Found user by direct username lookup:', directResult.Username);
            return directResult;
        } catch (directError) {
            if (directError.code !== 'UserNotFoundException') {
                console.log('Direct adminGetUser failed with non-404 error:', directError.message);
            }
        }
        
        // If both methods fail, return null
        console.log('User not found by preferred_username or direct username lookup');
        return null;
        
    } catch (error) {
        if (error.code === 'UserNotFoundException') {
            console.log('User not found:', username);
            return null;
        }
        console.error('Error in findUserByPreferredUsername:', error);
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
    
    // Use preferred_username if available, otherwise use the extracted matrix username
    const preferredUsername = attributes.preferred_username || matrixUsername;
    
    // Determine if user is active based on Cognito status
    // UserStatus can be: CONFIRMED, UNCONFIRMED, ARCHIVED, COMPROMISED, UNKNOWN, RESET_REQUIRED, FORCE_CHANGE_PASSWORD
    // Enabled field indicates if user is enabled/disabled
    const isActive = cognitoUser.Enabled !== false && 
                     cognitoUser.UserStatus === 'CONFIRMED';
    
    return {
        matrix_user_id: matrixUserId,
        matrix_username: preferredUsername,
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
        updated_at: cognitoUser.UserLastModifiedDate?.toISOString(),
        // Add Cognito status fields
        user_status: cognitoUser.UserStatus,
        is_enabled: cognitoUser.Enabled !== false,
        is_active: isActive,
        // Add approval status for document review
        approval_status: attributes['custom:approval_status'] || 'pending'
    };
} 