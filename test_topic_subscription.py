#!/usr/bin/env python3
"""
Test script to subscribe to Firebase topics for push notification testing
"""

import requests
import json

def subscribe_to_topic(server_key, token, topic):
    """
    Subscribe a device token to a Firebase topic
    
    Args:
        server_key: Firebase Server Key
        token: FCM token of the device
        topic: Topic name to subscribe to
    """
    
    url = "https://iid.googleapis.com/iid/v1:batchAdd"
    
    headers = {
        "Authorization": f"key={server_key}",
        "Content-Type": "application/json"
    }
    
    data = {
        "to": f"/topics/{topic}",
        "registration_tokens": [token]
    }
    
    try:
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 200:
            result = response.json()
            if result.get("success_count") == 1:
                print(f"✅ Successfully subscribed to topic: {topic}")
                return True
            else:
                print(f"❌ Failed to subscribe to topic: {topic}")
                print(f"Error: {result}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Error subscribing to topic: {e}")
        return False

def main():
    print("🚀 SignOut Topic Subscription Test")
    print("=" * 40)
    
    server_key = input("Enter Firebase Server Key: ").strip()
    if not server_key:
        print("❌ Server key is required")
        return
    
    token = input("Enter FCM token: ").strip()
    if not token:
        print("❌ FCM token is required")
        return
    
    topic = input("Enter topic name (e.g., 'test'): ").strip()
    if not topic:
        print("❌ Topic name is required")
        return
    
    print(f"\nSubscribing {token[:20]}... to topic '{topic}'")
    subscribe_to_topic(server_key, token, topic)

if __name__ == "__main__":
    main() 