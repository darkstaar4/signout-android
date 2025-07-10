#!/usr/bin/env python3
"""
Test script for Firebase Cloud Messaging push notifications
"""

import requests
import json
import sys

def send_test_notification(server_key, token=None, topic=None):
    """
    Send a test push notification using Firebase Cloud Messaging
    
    Args:
        server_key: Firebase Server Key from Firebase Console
        token: FCM token (optional, use topic if not provided)
        topic: Topic name (optional, use token if not provided)
    """
    
    url = "https://fcm.googleapis.com/fcm/send"
    
    headers = {
        "Authorization": f"key={server_key}",
        "Content-Type": "application/json"
    }
    
    # Test message data
    data = {
        "room_id": "test_room_123",
        "sender_name": "Test User",
        "message": "This is a test push notification from SignOut!"
    }
    
    # Notification payload
    notification = {
        "title": "Test User",
        "body": "This is a test push notification from SignOut!",
        "sound": "default"
    }
    
    # Build the message
    message = {
        "data": data,
        "notification": notification,
        "priority": "high",
        "content_available": True
    }
    
    if token:
        message["to"] = token
        print(f"Sending to token: {token[:20]}...")
    elif topic:
        message["to"] = f"/topics/{topic}"
        print(f"Sending to topic: {topic}")
    else:
        print("Error: Either token or topic must be provided")
        return False
    
    try:
        response = requests.post(url, headers=headers, json=message)
        
        if response.status_code == 200:
            result = response.json()
            if result.get("success") == 1:
                print("✅ Push notification sent successfully!")
                print(f"Message ID: {result.get('message_id')}")
                return True
            else:
                print("❌ Failed to send push notification")
                print(f"Error: {result}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Error sending push notification: {e}")
        return False

def main():
    print("🚀 SignOut Push Notification Test")
    print("=" * 40)
    
    # You'll need to get the Server Key from Firebase Console
    # Go to Project Settings > Cloud Messaging > Server key
    server_key = input("Enter Firebase Server Key: ").strip()
    
    if not server_key:
        print("❌ Server key is required")
        return
    
    print("\nChoose test method:")
    print("1. Send to specific FCM token")
    print("2. Send to topic (for testing)")
    
    choice = input("Enter choice (1 or 2): ").strip()
    
    if choice == "1":
        token = input("Enter FCM token: ").strip()
        if not token:
            print("❌ FCM token is required")
            return
        send_test_notification(server_key, token=token)
    elif choice == "2":
        topic = input("Enter topic name (e.g., 'test'): ").strip()
        if not topic:
            print("❌ Topic name is required")
            return
        send_test_notification(server_key, topic=topic)
    else:
        print("❌ Invalid choice")

if __name__ == "__main__":
    main() 