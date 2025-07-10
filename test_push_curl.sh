#!/bin/bash

# SignOut Push Notification Test Script
# This script helps test Firebase Cloud Messaging

echo "🚀 SignOut Push Notification Test"
echo "=================================="

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
echo "Choose test method:"
echo "1. Send to specific FCM token"
echo "2. Send to topic (for testing)"

read -p "Enter choice (1 or 2): " CHOICE

if [ "$CHOICE" = "1" ]; then
    read -p "Enter FCM token: " FCM_TOKEN
    if [ -z "$FCM_TOKEN" ]; then
        echo "❌ FCM token is required"
        exit 1
    fi
    
    echo "Sending to token: ${FCM_TOKEN:0:20}..."
    
    curl -X POST \
        -H "Authorization: key=$SERVER_KEY" \
        -H "Content-Type: application/json" \
        -d '{
            "to": "'$FCM_TOKEN'",
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

elif [ "$CHOICE" = "2" ]; then
    read -p "Enter topic name (e.g., 'test'): " TOPIC
    if [ -z "$TOPIC" ]; then
        echo "❌ Topic name is required"
        exit 1
    fi
    
    echo "Sending to topic: $TOPIC"
    
    curl -X POST \
        -H "Authorization: key=$SERVER_KEY" \
        -H "Content-Type: application/json" \
        -d '{
            "to": "/topics/'$TOPIC'",
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

else
    echo "❌ Invalid choice"
    exit 1
fi

echo ""
echo "✅ Test completed! Check your device for the notification." 