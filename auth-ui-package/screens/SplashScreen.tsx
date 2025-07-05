import { useEffect } from 'react';
import { View, Text, StyleSheet, Image, ActivityIndicator, SafeAreaView, TouchableOpacity } from 'react-native';
import { router } from 'expo-router';
import { useAuth } from '@/context/AuthContext';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function Index() {
  const { status, session, signOut } = useAuth();

  useEffect(() => {
    const timeout = setTimeout(async () => {
      console.log('Splash screen timeout reached. Status:', status);
      
      if (status === 'loading') {
        console.log('Status still loading, forcing navigation to auth');
        // Force clear any corrupted auth state
        try {
          await AsyncStorage.removeItem('@has_logged_out');
          await AsyncStorage.removeItem('@temp_user_data');
          console.log('Forced clear of auth data');
        } catch (error) {
          console.error('Error clearing auth data:', error);
        }
        router.replace('/auth/phone');
        return;
      }
      
      if (status === 'unauthenticated') {
        console.log('Status is unauthenticated, navigating to auth');
        router.replace('/auth/phone');
      } else if (status === 'authenticated') {
        if (session?.isApproved) {
          router.replace('/(tabs)');
        } else {
          router.replace('/auth/pending');
        }
      }
    }, 3000); // Increased timeout to 3 seconds

    return () => clearTimeout(timeout);
  }, [status, session]);

  // Add a fallback timeout for if status gets stuck
  useEffect(() => {
    const fallbackTimeout = setTimeout(async () => {
      if (status === 'loading') {
        console.log('Fallback timeout: forcing navigation after 5 seconds');
        // Force clear any corrupted auth state
        try {
          await AsyncStorage.removeItem('@has_logged_out');
          await AsyncStorage.removeItem('@temp_user_data');
          console.log('Fallback: Forced clear of auth data');
        } catch (error) {
          console.error('Fallback: Error clearing auth data:', error);
        }
        router.replace('/auth/phone');
      }
    }, 5000);

    return () => clearTimeout(fallbackTimeout);
  }, [status]);

  // Force navigation if we detect authentication errors
  useEffect(() => {
    const forceNavigationTimeout = setTimeout(() => {
      console.log('Force navigation timeout: Current status is', status);
      if (status === 'loading' || status === 'unauthenticated') {
        console.log('Force navigating to auth screen');
        router.replace('/auth/phone');
      }
    }, 8000); // 8 second absolute timeout

    return () => clearTimeout(forceNavigationTimeout);
  }, [status]);

  return (
    <SafeAreaView style={styles.safeArea}>
    <View style={styles.container}>
      <View style={styles.gradientBackground}>
        <View style={styles.overlay} />
      </View>
      <View style={styles.content}>
        <View style={styles.logoContainer}>
          <Image
            source={require('@/assets/images/SignoutSquareLogo.png')}
            style={styles.logo}
            resizeMode="contain"
          />
        </View>
        <Text style={styles.title}>Signout</Text>
        <Text style={styles.subtitle}>Secure Healthcare Messaging</Text>
        <ActivityIndicator size="large" color="#ffffff" style={styles.loader} />
        <Text style={styles.statusText}>Status: {status}</Text>
        <Text style={styles.statusText}>App is Running!</Text>
        
        {/* Matrix Rust SDK Test Button */}
        <TouchableOpacity 
          style={styles.testButton}
          onPress={() => router.push('/matrix-test')}
        >
          <Text style={styles.testButtonText}>Test Matrix Rust SDK</Text>
        </TouchableOpacity>
      </View>
    </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0F172A',
  },
  container: {
    flex: 1,
    position: 'relative',
  },
  gradientBackground: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#0F172A', // Navy fallback
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(15, 23, 42, 0.8)', // Navy overlay
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  logoContainer: {
    width: 120,
    height: 120,
    marginBottom: 32,
    borderRadius: 30,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  logo: {
    width: 80,
    height: 80,
  },
  title: {
    fontSize: 36,
    fontWeight: '700',
    color: '#ffffff',
    marginBottom: 8,
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 18,
    color: '#ffffff',
    marginBottom: 40,
    opacity: 0.9,
  },
  loader: {
    marginTop: 20,
  },
  statusText: {
    fontSize: 14,
    color: '#ffffff',
    marginTop: 20,
    opacity: 0.7,
  },
  testButton: {
    backgroundColor: '#ffffff',
    padding: 12,
    borderRadius: 8,
    marginTop: 20,
  },
  testButtonText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
  },
});