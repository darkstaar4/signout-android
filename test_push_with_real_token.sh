#!/bin/bash

# Test Push Notification with Real FCM Token
# This script uses the actual FCM token from the emulator

echo "🚀 SignOut Push Notification Test (Real Token)"
echo "=============================================="

# Real FCM token from the emulator logs
REAL_TOKEN="e_y-7RMGQR6IwBPwSIWbnx:APA91bHq5-WZKFYLiQcBMyuOF1fho2FvNNms1bNq7f4QovkVd-pfAZglIVtMgA0SrpzdylDegjgQHPw630tNKSejZSvXPzJ-Dln6hXrrEu2zIjYaE7-Bc2M"

echo "Using real FCM token: ${REAL_TOKEN:0:20}..."
echo ""

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
echo "Sending test notification to real device..."

curl -X POST \
    -H "Authorization: key=$SERVER_KEY" \
    -H "Content-Type: application/json" \
    -d '{
        "to": "'$REAL_TOKEN'",
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
echo "✅ Test completed! Check your emulator for the notification."
echo ""
echo "Note: You should see a notification appear on the emulator." 