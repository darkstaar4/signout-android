const AWS = require('aws-sdk');
const cognito = new AWS.CognitoIdentityServiceProvider();

// Environment variables
const USER_POOL_ID = process.env.USER_POOL_ID;
const CORS_ORIGIN = process.env.CORS_ORIGIN || '*';
const MATRIX_DOMAIN = process.env.MATRIX_DOMAIN || 'signout.io';

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
        const { query, limit = 10 } = event.queryStringParameters || {};
        
        if (!query) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({ 
                    error: 'query parameter is required' 
                })
            };
        }
        
        console.log('Searching for users with query:', query, 'limit:', limit);
        
        // Search for users in Cognito User Pool
        const users = await searchUsers(query, parseInt(limit));
        
        // Convert Cognito users to our format
        const cognitoUsers = users.map(user => convertCognitoUser(user));
        
        return {
            statusCode: 200,
            headers: corsHeaders,
            body: JSON.stringify({
                users: cognitoUsers,
                total: cognitoUsers.length,
                query: query,
                limit: parseInt(limit)
            })
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

async function searchUsers(query, limit) {
    try {
        // Try multiple search patterns to find users
        const searchPatterns = [
            `"given_name" ^= "${query}"`,
            `"family_name" ^= "${query}"`,
            `"preferred_username" ^= "${query}"`
        ];
        
        const allUsers = [];
        const seenUsernames = new Set();
        
        // Try each search pattern
        for (const filter of searchPatterns) {
            try {
                const params = {
                    UserPoolId: USER_POOL_ID,
                    Limit: Math.min(limit, 60), // Cognito API limit
                    Filter: filter
                };

                const result = await cognito.listUsers(params).promise();
                
                result.Users.forEach(user => {
                    if (!seenUsernames.has(user.Username)) {
                        seenUsernames.add(user.Username);
                        allUsers.push(user);
                    }
                });
            } catch (filterError) {
                console.log(`Filter "${filter}" failed:`, filterError.message);
                // Continue with next filter
            }
        }
        
        // If no results from filters, try without filter and do client-side filtering
        if (allUsers.length === 0) {
            try {
                const params = {
                    UserPoolId: USER_POOL_ID,
                    Limit: Math.min(limit * 5, 60) // Get more users to filter client-side
                };
                
                const result = await cognito.listUsers(params).promise();
                
                // Filter users based on query
                const filteredUsers = result.Users.filter(user => {
                    const attributes = {};
                    if (user.Attributes) {
                        user.Attributes.forEach(attr => {
                            attributes[attr.Name] = attr.Value;
                        });
                    }
                    
                    const searchableFields = [
                        user.Username,
                        attributes.given_name,
                        attributes.family_name,
                        attributes.preferred_username,
                        attributes.email
                    ];
                    
                    const queryLower = query.toLowerCase();
                    
                    return searchableFields.some(field => 
                        field && field.toLowerCase().includes(queryLower)
                    );
                });
                
                return filteredUsers.slice(0, limit);
            } catch (noFilterError) {
                console.error('Error searching users without filter:', noFilterError);
                throw noFilterError;
            }
        }
        
        return allUsers.slice(0, limit);
        
    } catch (error) {
        console.error('Error searching users:', error);
        throw error;
    }
}

function convertCognitoUser(cognitoUser) {
    const attributes = {};
    
    // Convert Cognito attributes to key-value pairs
    if (cognitoUser.Attributes) {
        cognitoUser.Attributes.forEach(attr => {
            attributes[attr.Name] = attr.Value;
        });
    }
    
    // Use preferred_username if available, otherwise use Username
    const username = attributes.preferred_username || cognitoUser.Username;
    const matrixUserId = `@${username}:${MATRIX_DOMAIN}`;
    
    // Determine if user is active based on Cognito status
    // UserStatus can be: CONFIRMED, UNCONFIRMED, ARCHIVED, COMPROMISED, UNKNOWN, RESET_REQUIRED, FORCE_CHANGE_PASSWORD
    // Enabled field indicates if user is enabled/disabled
    const isActive = cognitoUser.Enabled !== false && 
                     cognitoUser.UserStatus === 'CONFIRMED';
    
    return {
        matrix_user_id: matrixUserId,
        matrix_username: username,
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