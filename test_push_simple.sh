#!/bin/bash

# Simple Push Notification Test for SignOut
# This script sends a test push notification to the test token

echo "🚀 SignOut Push Notification Test (Simple)"
echo "=========================================="

# Default test token (matches the one in TestPushActivity)
TEST_TOKEN="test_token_12345"

# You need to get the Server Key from Firebase Console:
# 1. Go to https://console.firebase.google.com/
# 2. Select your project (signout-66af0)
# 3. Go to Project Settings > Cloud Messaging
# 4. Copy the Server key

read -p "Enter Firebase Server Key: " SERVER_KEY

if [ -z "$SERVER_KEY" ]; then
    echo "❌ Server key is required"
    exit 1
fi

echo ""
echo "Using test token: $TEST_TOKEN"
echo "Sending test notification..."

curl -X POST \
    -H "Authorization: key=$SERVER_KEY" \
    -H "Content-Type: application/json" \
    -d '{
        "to": "'$TEST_TOKEN'",
        "data": {
            "room_id": "test_room_123",
            "sender_name": "Test User",
            "message": "This is a test push notification from SignOut!"
        },
        "notification": {
            "title": "Test User",
            "body": "This is a test push notification from SignOut!",
            "sound": "default"
        },
        "priority": "high",
        "content_available": true
    }' \
    https://fcm.googleapis.com/fcm/send

echo ""
echo "✅ Test completed! Check your device for the notification."
echo ""
echo "Note: If you don't see the notification, make sure:"
echo "1. The app is installed and running"
echo "2. You've set the test token in the TestPushActivity"
echo "3. The notification channel is created" 