import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { router } from 'expo-router';
import { Clock, LogOut, Bell } from 'lucide-react-native';
import { useAuth } from '@/context/AuthContext';

export default function PendingScreen() {
  const { signOut } = useAuth();

  const handleSignOut = async () => {
    await signOut();
    router.replace('/auth/phone');
  };

  return (
    <View style={styles.container}>
      <Image
        source={{ uri: 'https://images.pexels.com/photos/3760067/pexels-photo-3760067.jpeg' }}
        style={styles.backgroundImage}
        blurRadius={3}
      />
      <View style={styles.overlay} />
      
      <View style={styles.content}>
        <View style={styles.card}>
          <Clock color="#2C5282" size={60} style={styles.icon} />
          
          <Text style={styles.title}>Verification In Progress</Text>
          
          <Text style={styles.description}>
            Your credentials are being reviewed by our team. This typically takes 1-2 business days.
          </Text>
          
          <View style={styles.infoBox}>
            <Text style={styles.infoBoxTitle}>What happens next?</Text>
            <Text style={styles.infoBoxText}>
              Once your credentials are verified, you'll receive a push notification and can begin using Signout to securely communicate with other healthcare professionals.
            </Text>
          </View>
          
          <View style={styles.notificationBox}>
            <Bell color="#38A169" size={20} style={styles.notificationIcon} />
            <Text style={styles.notificationText}>
              You'll receive a push notification when your account is approved
            </Text>
          </View>
          
          <TouchableOpacity style={styles.signOutButton} onPress={handleSignOut}>
            <LogOut color="#4A5568" size={18} style={styles.signOutIcon} />
            <Text style={styles.signOutButtonText}>Logout</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    position: 'relative',
  },
  backgroundImage: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(44, 82, 130, 0.85)',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 32,
    width: '100%',
    maxWidth: 400,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  icon: {
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#2D3748',
    textAlign: 'center',
    marginBottom: 16,
  },
  description: {
    fontSize: 16,
    color: '#4A5568',
    textAlign: 'center',
    marginBottom: 24,
  },
  infoBox: {
    backgroundColor: '#EBF8FF',
    borderRadius: 8,
    padding: 16,
    width: '100%',
    marginBottom: 20,
  },
  infoBoxTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2C5282',
    marginBottom: 8,
  },
  infoBoxText: {
    fontSize: 14,
    color: '#4A5568',
    lineHeight: 20,
  },
  notificationBox: {
    backgroundColor: '#F0FFF4',
    borderRadius: 8,
    padding: 16,
    width: '100%',
    marginBottom: 32,
    flexDirection: 'row',
    alignItems: 'center',
  },
  notificationIcon: {
    marginRight: 12,
  },
  notificationText: {
    fontSize: 14,
    color: '#38A169',
    flex: 1,
    lineHeight: 20,
  },
  signOutButton: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  signOutIcon: {
    marginRight: 8,
  },
  signOutButtonText: {
    color: '#4A5568',
    fontSize: 14,
    fontWeight: '500',
  },
});