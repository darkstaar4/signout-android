import { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { router } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { Camera, FilePlus, FileText, Upload } from 'lucide-react-native';
import { registrationRequestsApi, CreateRegistrationRequestData } from '@/services/registrationRequestsApi';
import AsyncStorage from '@react-native-async-storage/async-storage';

type DocumentInfo = {
  uri: string;
  name: string;
  type: string;
  size?: number;
};

type StoredUserData = {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  npiNumber?: string;
  professionalTitle: string;
  specialty: string;
  officeAddress: string;
  officeCity: string;
  officeState: string;
  officeZip: string;
  country: string;
  password: string;
};

export default function CredentialsScreen() {
  const [idDocument, setIdDocument] = useState<DocumentInfo | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [userData, setUserData] = useState<StoredUserData | null>(null);
  const [isLoadingUserData, setIsLoadingUserData] = useState(true);

  // Load user data from AsyncStorage on component mount
  useEffect(() => {
    const loadUserData = async () => {
      try {
        const userDataString = await AsyncStorage.getItem('userData');
        if (userDataString) {
          const parsedUserData = JSON.parse(userDataString);
          setUserData(parsedUserData);
          console.log('Loaded user data from AsyncStorage:', parsedUserData);
        } else {
          console.error('No user data found in AsyncStorage');
          Alert.alert('Error', 'User data not found. Please try registering again.');
          router.replace('/auth/register');
        }
      } catch (error) {
        console.error('Error loading user data:', error);
        Alert.alert('Error', 'Failed to load user data. Please try registering again.');
        router.replace('/auth/register');
      } finally {
        setIsLoadingUserData(false);
      }
    };

    loadUserData();
  }, []);

  const pickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['application/pdf', 'image/*'],
        copyToCacheDirectory: true,
      });
      
      if (!result.canceled) {
        const asset = result.assets[0];
        setIdDocument({
          uri: asset.uri,
          name: asset.name || 'document',
          type: asset.mimeType || 'application/octet-stream',
          size: asset.size,
        });
      }
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to pick document');
    }
  };

  const takePicture = async () => {
    try {
      const { status } = await ImagePicker.requestCameraPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission Required', 'Camera permission is required to take pictures');
        return;
      }
      
      const result = await ImagePicker.launchCameraAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: true,
        quality: 0.8,
      });
      
      if (!result.canceled) {
        setIdDocument({
          uri: result.assets[0].uri,
          name: 'id_document.jpg',
          type: 'image/jpeg',
          size: result.assets[0].fileSize,
        });
      }
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to take picture');
    }
  };

  const handleSubmit = async () => {
    // Check if ID document is uploaded
    if (!idDocument) {
      Alert.alert('Missing ID Document', 'Please upload an ID document');
      return;
    }
    
    // Check if user data is available
    if (!userData) {
      Alert.alert('Error', 'User information not available');
      return;
    }
    
    setIsLoading(true);
    try {
      // Prepare the registration request data
      const registrationData: CreateRegistrationRequestData = {
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        email: userData.email || '',
        phoneNumber: userData.phoneNumber || '',
        npiNumber: userData.npiNumber || '', // NPI from registration screen
        specialty: userData.specialty || 'General Medicine',
        documents: {
          idDocument: {
            name: idDocument.name,
            type: idDocument.type,
            size: idDocument.size,
            // Note: In a real implementation, you'd upload the file to S3 first
            // and store the S3 URL here
          }
        }
      };
      
      // Create registration request via API
      const result = await registrationRequestsApi.createRegistrationRequest(registrationData);
      
      console.log('Registration request created:', result);
      
      // Check if this is the super admin user
      const isSuperAdmin = userData.email === 'nabil.baig@gmail.com';
      
      if (isSuperAdmin) {
        // Super admin goes directly to main app
        router.replace('/(tabs)');
      } else {
        // Regular users go to pending screen
        router.push('/auth/pending');
      }
    } catch (error) {
      console.error('Error creating registration request:', error);
      Alert.alert('Error', 'Failed to submit registration request. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // Show loading state while user data is being loaded
  if (isLoadingUserData) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>Loading...</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.scrollContent}>
      <View style={styles.container}>
        <Text style={styles.title}>Upload Your Credentials</Text>
        
        {/* Healthcare Workers Only Notice */}
        <View style={styles.healthcareNotice}>
          <Text style={styles.healthcareNoticeTitle}>
            🏥 This app is ONLY for healthcare workers
          </Text>
          <Text style={styles.healthcareNoticeText}>
            Your account activation depends on verification. You must upload your Work ID, School ID, or Driver's License.
          </Text>
        </View>
        
        <Text style={styles.subtitle}>
          To verify you're in the healthcare field, please provide the following information
        </Text>
        
        {/* ID Document Section */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <FileText color="#2C5282" size={24} />
            <View style={styles.sectionTitleContainer}>
              <Text style={styles.sectionTitle}>ID Document</Text>
              <Text style={styles.sectionDescription}>
                Upload a work ID, school ID, driver's license, or copy of medical license
              </Text>
            </View>
          </View>
          
          {idDocument ? (
            <View style={styles.uploadedDocument}>
              <FileText color="#38A169" size={20} />
              <Text style={styles.uploadedDocumentName} numberOfLines={1}>
                {idDocument.name}
              </Text>
              <TouchableOpacity
                style={styles.removeButton}
                onPress={() => setIdDocument(null)}
              >
                <Text style={styles.removeButtonText}>Remove</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.documentActions}>
              <TouchableOpacity
                style={styles.documentActionButton}
                onPress={pickDocument}
              >
                <FilePlus color="#2C5282" size={16} />
                <Text style={styles.documentActionText}>Upload PDF/Image</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                style={styles.documentActionButton}
                onPress={takePicture}
              >
                <Camera color="#2C5282" size={16} />
                <Text style={styles.documentActionText}>Take Photo</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
        
        <Text style={styles.securityNote}>
          Your documents are securely encrypted and only accessible to our verification team.
        </Text>
        
        <TouchableOpacity
          style={[
            styles.submitButton,
            (!idDocument) && styles.submitButtonDisabled,
            isLoading && styles.submitButtonLoading,
          ]}
          onPress={handleSubmit}
          disabled={!idDocument || isLoading}
        >
          <Upload color="#ffffff" size={20} style={styles.submitIcon} />
          <Text style={styles.submitButtonText}>
            {isLoading ? 'Uploading...' : 'Submit for Verification'}
          </Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContent: {
    flexGrow: 1,
  },
  container: {
    flex: 1,
    padding: 24,
    backgroundColor: '#ffffff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ffffff',
  },
  loadingText: {
    fontSize: 16,
    color: '#64748B',
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#2C5282',
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    color: '#4A5568',
    marginBottom: 32,
  },
  healthcareNotice: {
    backgroundColor: '#F0FFF4',
    padding: 16,
    borderRadius: 8,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#38A169',
  },
  healthcareNoticeTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#2C5282',
    marginBottom: 8,
  },
  healthcareNoticeText: {
    fontSize: 14,
    color: '#718096',
    lineHeight: 20,
  },
  section: {
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    backgroundColor: '#F7FAFC',
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitleContainer: {
    marginLeft: 12,
    flex: 1,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2D3748',
  },
  sectionDescription: {
    fontSize: 14,
    color: '#718096',
    marginTop: 4,
  },
  documentActions: {
    flexDirection: 'row',
    gap: 12,
  },
  documentActionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#EBF8FF',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    flex: 1,
    justifyContent: 'center',
  },
  documentActionText: {
    color: '#2C5282',
    marginLeft: 6,
    fontWeight: '500',
    fontSize: 14,
  },
  uploadedDocument: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F0FFF4',
    padding: 12,
    borderRadius: 8,
  },
  uploadedDocumentName: {
    color: '#38A169',
    marginLeft: 8,
    flex: 1,
    fontSize: 14,
  },
  removeButton: {
    backgroundColor: '#FED7D7',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  removeButtonText: {
    color: '#C53030',
    fontSize: 12,
    fontWeight: '500',
  },
  securityNote: {
    fontSize: 14,
    color: '#718096',
    marginBottom: 32,
    fontStyle: 'italic',
  },
  submitButton: {
    backgroundColor: '#2C5282',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 8,
    width: '100%',
  },
  submitButtonDisabled: {
    backgroundColor: '#90CDF4',
  },
  submitButtonLoading: {
    backgroundColor: '#4299E1',
  },
  submitIcon: {
    marginRight: 8,
  },
  submitButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
}); 