const AWS = require('aws-sdk');
const s3 = new AWS.S3();

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token',
    'Access-Control-Allow-Methods': 'OPTIONS,POST,GET'
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
        const { filename, contentType, username, operation } = JSON.parse(event.body || '{}');
        
        if (!filename || !username) {
            return {
                statusCode: 400,
                headers: corsHeaders,
                body: JSON.stringify({ 
                    error: 'Missing required fields: filename, username' 
                })
            };
        }
        
        console.log('Processing request for:', { filename, contentType, username, operation });
        
        const bucketName = 'signout-verification-documents';
        const key = `verification-documents/${filename}`;
        
        if (operation === 'download') {
            // Generate presigned URL for GET operation (download)
            const params = {
                Bucket: bucketName,
                Key: key,
                Expires: 3600 // 1 hour for downloads
            };
            
            const downloadUrl = s3.getSignedUrl('getObject', params);
            
            console.log('Generated download URL for:', key);
            
            return {
                statusCode: 200,
                headers: corsHeaders,
                body: JSON.stringify({
                    downloadUrl,
                    fileUrl: `https://${bucketName}.s3.amazonaws.com/${key}`
                })
            };
        } else {
            // Default to upload operation
            if (!contentType) {
                return {
                    statusCode: 400,
                    headers: corsHeaders,
                    body: JSON.stringify({ 
                        error: 'Missing required field: contentType for upload operation' 
                    })
                };
            }
            
            // Generate presigned URL for PUT operation (upload)
            const params = {
                Bucket: bucketName,
                Key: key,
                Expires: 300, // 5 minutes for uploads
                ContentType: contentType,
                ACL: 'private'
            };
            
            const uploadUrl = s3.getSignedUrl('putObject', params);
            const fileUrl = `https://${bucketName}.s3.amazonaws.com/${key}`;
            
            console.log('Generated upload URLs:', { uploadUrl, fileUrl });
            
            return {
                statusCode: 200,
                headers: corsHeaders,
                body: JSON.stringify({
                    uploadUrl,
                    fileUrl
                })
            };
        }
        
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