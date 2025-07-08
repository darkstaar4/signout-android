import { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, Image, Platform, KeyboardAvoidingView, ScrollView, Alert, Modal, SafeAreaView } from 'react-native';
// import { router } from 'expo-router'; // You'll need to implement navigation
// import { Shield } from 'lucide-react-native'; // Install: npm install lucide-react-native
// import { useAuth } from '@/context/AuthContext'; // Implement your AuthContext
// import USAPrideBanner from '@/components/USAPrideBanner'; // Optional component

// TODO: Implement these in your project
const router = {
  replace: (path: string) => console.log('Navigate to:', path),
  push: (path: string) => console.log('Navigate to:', path),
};

const Shield = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color }} />
);

const useAuth = () => ({
  login: async (email: string, password: string) => {
    console.log('Login:', email, password);
    // Implement your login logic
  }
});

const USAPrideBanner = ({ compact, forceLightTheme }: any) => null; // Optional

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotPasswordEmail, setForgotPasswordEmail] = useState('');
  const [isForgotPasswordLoading, setIsForgotPasswordLoading] = useState(false);
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [tempPassword, setTempPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isResetPasswordLoading, setIsResetPasswordLoading] = useState(false);
  const { login } = useAuth();

  // Force light mode colors - always use light theme
  const colors = {
    background: '#ffffff',
    surface: '#ffffff',
    primary: '#0EA5E9',
    text: '#000000',        // Pure black for maximum visibility
    textSecondary: '#333333', // Dark gray for secondary text
    border: '#E2E8F0',
    error: '#EF4444',
    modalOverlay: 'rgba(0, 0, 0, 0.5)',
    modalBackground: '#fff',
    securityBackground: '#F0F9FF',
    logoBackground: 'rgba(14, 165, 233, 0.1)',
  };

  const handleLogin = async () => {
    if (!email || !password) {
      setError('Email and password are required.');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      await login(email, password);
      router.replace('/(tabs)'); // Replace with your main app route
    } catch (error: any) {
      console.error('=== LOGIN ERROR ===');
      console.error('Error name:', error.name);
      console.error('Error message:', error.message);
      console.error('Full error:', error);
      
      // Handle different error types
      if (error.message) {
        setError(error.message);
      } else if (error.name === 'UserNotConfirmedException') {
        setError('Please verify your email address before signing in.');
      } else if (error.name === 'NotAuthorizedException') {
        setError('Invalid email or password.');
      } else if (error.name === 'UserNotFoundException') {
        setError('No account found with this email address.');
      } else {
        setError('Login failed. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = () => {
    router.push('/auth/register'); // Replace with your register route
  };

  const handleForgotPassword = async () => {
    if (!forgotPasswordEmail) {
      Alert.alert('Error', 'Please enter your email address.');
      return;
    }

    setIsForgotPasswordLoading(true);
    try {
      console.log('Sending forgot password request for:', forgotPasswordEmail);
      
      // TODO: Implement forgot password API call
      const response = await fetch(`YOUR_API_ENDPOINT/forgot-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: forgotPasswordEmail }),
      });

      const data = await response.json();

      if (response.ok) {
        Alert.alert(
          'Success', 
          'A temporary password has been sent to your email. Please check your inbox.'
        );
        setShowForgotPassword(false);
        setForgotPasswordEmail('');
      } else {
        Alert.alert('Error', data.error || 'Failed to send password reset email.');
      }
    } catch (error) {
      console.error('Forgot password error:', error);
      Alert.alert('Error', 'Failed to send password reset email. Please try again.');
    } finally {
      setIsForgotPasswordLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (!tempPassword || !newPassword || !confirmPassword) {
      Alert.alert('Error', 'All fields are required.');
      return;
    }

    if (newPassword !== confirmPassword) {
      Alert.alert('Error', 'New passwords do not match.');
      return;
    }

    if (newPassword.length < 8) {
      Alert.alert('Error', 'Password must be at least 8 characters long.');
      return;
    }

    setIsResetPasswordLoading(true);
    try {
      // TODO: Implement reset password API call
      const response = await fetch(`YOUR_API_ENDPOINT/reset-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          email: forgotPasswordEmail,
          newPassword,
          confirmPassword 
        }),
      });

      const data = await response.json();

      if (response.ok) {
        Alert.alert('Success', 'Password has been reset successfully. You can now log in with your new password.');
        setShowResetPassword(false);
        setTempPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setForgotPasswordEmail('');
      } else {
        Alert.alert('Error', data.error || 'Failed to reset password.');
      }
    } catch (error) {
      console.error('Reset password error:', error);
      Alert.alert('Error', 'Failed to reset password. Please try again.');
    } finally {
      setIsResetPasswordLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.background }]}>
      <View style={styles.mainContainer}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View style={[styles.container, { backgroundColor: colors.background }]}>
            <View style={[styles.logoContainer, { backgroundColor: colors.logoBackground }]}>
              <Image
                source={require('../assets/SignoutSquareLogo.png')} // Update path as needed
                style={styles.logo}
                resizeMode="contain"
              />
            </View>
            <Text style={[styles.title, { color: colors.text }]}>Sign in to Your App</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
              Secure messaging platform for healthcare providers
            </Text>

            <View style={[styles.securityInfo, { backgroundColor: colors.securityBackground }]}>
              <Shield color="#0EA5E9" size={20} />
              <Text style={[styles.securityText, { color: colors.primary }]}>
                Encrypted, secure, and HIPAA-compliant
              </Text>
            </View>

            <View style={styles.inputContainer}>
              <Text style={[styles.label, { color: colors.text }]}>Email Address</Text>
              <TextInput
                style={[styles.input, { 
                  color: colors.text, 
                  backgroundColor: colors.surface, 
                  borderColor: colors.border 
                }]}
                placeholder="you@email.com"
                placeholderTextColor={colors.textSecondary}
                keyboardType="email-address"
                autoCapitalize="none"
                value={email}
                onChangeText={setEmail}
              />
              <Text style={[styles.label, { color: colors.text }]}>Password</Text>
              <TextInput
                style={[styles.input, { 
                  color: colors.text, 
                  backgroundColor: colors.surface, 
                  borderColor: colors.border 
                }]}
                placeholder="Password"
                placeholderTextColor={colors.textSecondary}
                secureTextEntry
                value={password}
                onChangeText={setPassword}
              />
            </View>

            {error ? <Text style={[styles.error, { color: colors.error }]}>{error}</Text> : null}

            <TouchableOpacity
              style={[
                styles.continueButton,
                (!email || !password) && styles.continueButtonDisabled,
                isLoading && styles.continueButtonLoading,
              ]}
              onPress={handleLogin}
              disabled={!email || !password || isLoading}
            >
              <Text style={styles.continueButtonText}>
                {isLoading ? 'Signing in...' : 'Sign In'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity onPress={() => setShowForgotPassword(true)} style={styles.forgotPasswordCta}>
              <Text style={styles.forgotPasswordText}>Forgot your password?</Text>
            </TouchableOpacity>

            <TouchableOpacity onPress={handleRegister} style={styles.registerCta}>
              <Text style={[styles.registerText, { color: colors.textSecondary }]}>Don't have an account? <Text style={styles.registerNow}>Register now</Text></Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Forgot Password Modal */}
      <Modal
        visible={showForgotPassword}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowForgotPassword(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: colors.modalBackground }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Forgot Password</Text>
            <Text style={[styles.modalSubtitle, { color: colors.textSecondary }]}>
              Enter your email address and we'll send you a temporary password.
            </Text>
            
            <TextInput
              style={[styles.modalInput, { 
                color: colors.text, 
                backgroundColor: colors.surface, 
                borderColor: colors.border 
              }]}
              placeholder="Enter your email"
              placeholderTextColor={colors.textSecondary}
              keyboardType="email-address"
              autoCapitalize="none"
              value={forgotPasswordEmail}
              onChangeText={setForgotPasswordEmail}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalButtonSecondary}
                onPress={() => setShowForgotPassword(false)}
              >
                <Text style={styles.modalButtonSecondaryText}>Cancel</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                style={[
                  styles.modalButtonPrimary,
                  isForgotPasswordLoading && styles.modalButtonLoading
                ]}
                onPress={handleForgotPassword}
                disabled={isForgotPasswordLoading}
              >
                <Text style={styles.modalButtonPrimaryText}>
                  {isForgotPasswordLoading ? 'Sending...' : 'Send Reset Email'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Reset Password Modal */}
      <Modal
        visible={showResetPassword}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowResetPassword(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: colors.modalBackground }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Reset Password</Text>
            <Text style={[styles.modalSubtitle, { color: colors.textSecondary }]}>
              Enter the temporary password from your email and set a new password.
            </Text>
            
            <TextInput
              style={[styles.modalInput, { 
                color: colors.text, 
                backgroundColor: colors.surface, 
                borderColor: colors.border 
              }]}
              placeholder="Temporary password from email"
              placeholderTextColor={colors.textSecondary}
              secureTextEntry
              value={tempPassword}
              onChangeText={setTempPassword}
            />
            
            <TextInput
              style={[styles.modalInput, { 
                color: colors.text, 
                backgroundColor: colors.surface, 
                borderColor: colors.border 
              }]}
              placeholder="New password"
              placeholderTextColor={colors.textSecondary}
              secureTextEntry
              value={newPassword}
              onChangeText={setNewPassword}
            />
            
            <TextInput
              style={[styles.modalInput, { 
                color: colors.text, 
                backgroundColor: colors.surface, 
                borderColor: colors.border 
              }]}
              placeholder="Confirm new password"
              placeholderTextColor={colors.textSecondary}
              secureTextEntry
              value={confirmPassword}
              onChangeText={setConfirmPassword}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.modalButtonSecondary}
                onPress={() => setShowResetPassword(false)}
              >
                <Text style={styles.modalButtonSecondaryText}>Cancel</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                style={[
                  styles.modalButtonPrimary,
                  isResetPasswordLoading && styles.modalButtonLoading
                ]}
                onPress={handleResetPassword}
                disabled={isResetPasswordLoading}
              >
                <Text style={styles.modalButtonPrimaryText}>
                  {isResetPasswordLoading ? 'Resetting...' : 'Reset Password'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      <USAPrideBanner compact forceLightTheme />
    </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  scrollContent: {
    flexGrow: 1,
  },
  container: {
    flex: 1,
    padding: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoContainer: {
    width: 120,
    height: 120,
    marginBottom: 32,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logo: {
    width: 80,
    height: 80,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 32,
  },
  securityInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 8,
    marginBottom: 32,
    maxWidth: 320,
  },
  securityText: {
    marginLeft: 8,
    fontSize: 14,
    fontWeight: '500',
  },
  inputContainer: {
    width: '100%',
    maxWidth: 320,
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  input: {
    padding: 14,
    fontSize: 16,
    borderWidth: 1,
    borderRadius: 8,
    marginBottom: 16,
  },
  continueButton: {
    backgroundColor: '#0EA5E9',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 8,
    width: '100%',
    maxWidth: 320,
    alignItems: 'center',
    marginBottom: 16,
    shadowColor: '#0EA5E9',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 4,
  },
  continueButtonDisabled: {
    backgroundColor: '#94A3B8',
  },
  continueButtonLoading: {
    opacity: 0.7,
  },
  continueButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  registerCta: {
    marginTop: 8,
  },
  registerText: {
    fontSize: 15,
    textAlign: 'center',
  },
  registerNow: {
    color: '#0EA5E9',
    fontWeight: '700',
  },
  error: {
    marginBottom: 16,
  },
  forgotPasswordCta: {
    marginTop: 8,
    marginBottom: 16,
  },
  forgotPasswordText: {
    color: '#0EA5E9',
    fontSize: 15,
    textAlign: 'center',
    fontWeight: '600',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    padding: 24,
    borderRadius: 8,
    width: '80%',
    maxWidth: 320,
  },
  modalTitle: {
    fontSize: 28,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 12,
  },
  modalSubtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 32,
  },
  modalInput: {
    padding: 14,
    fontSize: 16,
    borderWidth: 1,
    borderRadius: 8,
    marginBottom: 16,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
  },
  modalButtonSecondary: {
    backgroundColor: '#94A3B8',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    flex: 1,
    alignItems: 'center',
  },
  modalButtonSecondaryText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  modalButtonPrimary: {
    backgroundColor: '#0EA5E9',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    flex: 1,
    alignItems: 'center',
  },
  modalButtonPrimaryText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  modalButtonLoading: {
    opacity: 0.7,
  },
  safeArea: {
    flex: 1,
  },
  mainContainer: {
    flex: 1,
  },
});