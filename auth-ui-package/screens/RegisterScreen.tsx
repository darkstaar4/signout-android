import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, Image, Platform, KeyboardAvoidingView, ScrollView, Alert } from 'react-native';
import { router } from 'expo-router';
import { Shield, ArrowLeft } from 'lucide-react-native';
import { useAuth } from '@/context/AuthContext';
import PhoneNumberInput from '@/components/PhoneNumberInput';
import CustomDropdown, { DropdownOption } from '@/components/CustomDropdown';

const PROFESSIONAL_TITLES = [
  { label: 'MD', value: 'MD' },
  { label: 'DO', value: 'DO' },
  { label: 'PA-C', value: 'PA-C' },
  { label: 'NP', value: 'NP' },
  { label: 'RN', value: 'RN' },
  { label: 'Student', value: 'Student' },
];

const COUNTRIES = [
  { label: '🇺🇸 USA', value: 'USA' },
  { label: '🇨🇦 Canada', value: 'Canada' },
  { label: '🇲🇽 Mexico', value: 'Mexico' },
];

const US_STATES = [
  { label: '🏔️ Alabama', value: 'AL' },
  { label: '🏔️ Alaska', value: 'AK' },
  { label: '🏔️ Arizona', value: 'AZ' },
  { label: '🏔️ Arkansas', value: 'AR' },
  { label: '🏔️ California', value: 'CA' },
  { label: '🏔️ Colorado', value: 'CO' },
  { label: '🏔️ Connecticut', value: 'CT' },
  { label: '🏔️ Delaware', value: 'DE' },
  { label: '🏔️ Florida', value: 'FL' },
  { label: '🏔️ Georgia', value: 'GA' },
  { label: '🏔️ Hawaii', value: 'HI' },
  { label: '🏔️ Idaho', value: 'ID' },
  { label: '🏔️ Illinois', value: 'IL' },
  { label: '🏔️ Indiana', value: 'IN' },
  { label: '🏔️ Iowa', value: 'IA' },
  { label: '🏔️ Kansas', value: 'KS' },
  { label: '🏔️ Kentucky', value: 'KY' },
  { label: '🏔️ Louisiana', value: 'LA' },
  { label: '🏔️ Maine', value: 'ME' },
  { label: '🏔️ Maryland', value: 'MD' },
  { label: '🏔️ Massachusetts', value: 'MA' },
  { label: '🏔️ Michigan', value: 'MI' },
  { label: '🏔️ Minnesota', value: 'MN' },
  { label: '🏔️ Mississippi', value: 'MS' },
  { label: '🏔️ Missouri', value: 'MO' },
  { label: '🏔️ Montana', value: 'MT' },
  { label: '🏔️ Nebraska', value: 'NE' },
  { label: '🏔️ Nevada', value: 'NV' },
  { label: '🏔️ New Hampshire', value: 'NH' },
  { label: '🏔️ New Jersey', value: 'NJ' },
  { label: '🏔️ New Mexico', value: 'NM' },
  { label: '🏔️ New York', value: 'NY' },
  { label: '🏔️ North Carolina', value: 'NC' },
  { label: '🏔️ North Dakota', value: 'ND' },
  { label: '🏔️ Ohio', value: 'OH' },
  { label: '🏔️ Oklahoma', value: 'OK' },
  { label: '🏔️ Oregon', value: 'OR' },
  { label: '🏔️ Pennsylvania', value: 'PA' },
  { label: '🏔️ Rhode Island', value: 'RI' },
  { label: '🏔️ South Carolina', value: 'SC' },
  { label: '🏔️ South Dakota', value: 'SD' },
  { label: '🏔️ Tennessee', value: 'TN' },
  { label: '🏔️ Texas', value: 'TX' },
  { label: '🏔️ Utah', value: 'UT' },
  { label: '🏔️ Vermont', value: 'VT' },
  { label: '🏔️ Virginia', value: 'VA' },
  { label: '🏔️ Washington', value: 'WA' },
  { label: '🏔️ West Virginia', value: 'WV' },
  { label: '🏔️ Wisconsin', value: 'WI' },
  { label: '🏔️ Wyoming', value: 'WY' },
];

const SPECIALTIES = [
  { label: 'Student', value: 'Student' },
  { label: 'Other', value: 'Other' },
  { label: 'Addiction Medicine', value: 'Addiction Medicine' },
  { label: 'Anesthesiology', value: 'Anesthesiology' },
  { label: 'Cardiology', value: 'Cardiology' },
  { label: 'Dermatology', value: 'Dermatology' },
  { label: 'Emergency Medicine', value: 'Emergency Medicine' },
  { label: 'Endocrinology', value: 'Endocrinology' },
  { label: 'Family Medicine', value: 'Family Medicine' },
  { label: 'Gastroenterology', value: 'Gastroenterology' },
  { label: 'General Surgery', value: 'General Surgery' },
  { label: 'Heme/Onc', value: 'Heme/Onc' },
  { label: 'Hematology', value: 'Hematology' },
  { label: 'Hospitalist', value: 'Hospitalist' },
  { label: 'Infectious Disease', value: 'Infectious Disease' },
  { label: 'Internal Medicine', value: 'Internal Medicine' },
  { label: 'Interventional Radiology', value: 'Interventional Radiology' },
  { label: 'Nephrology', value: 'Nephrology' },
  { label: 'Neurology', value: 'Neurology' },
  { label: 'Neurosurgery', value: 'Neurosurgery' },
  { label: 'Obesity Medicine', value: 'Obesity Medicine' },
  { label: 'Obstetrics & Gynecology', value: 'Obstetrics & Gynecology' },
  { label: 'Oncology', value: 'Oncology' },
  { label: 'Ophthalmology', value: 'Ophthalmology' },
  { label: 'Orthopedic Surgery', value: 'Orthopedic Surgery' },
  { label: 'Otolaryngology', value: 'Otolaryngology' },
  { label: 'Pathology', value: 'Pathology' },
  { label: 'Pediatrics', value: 'Pediatrics' },
  { label: 'Plastic Surgery', value: 'Plastic Surgery' },
  { label: 'Psychiatry', value: 'Psychiatry' },
  { label: 'Pulmonology', value: 'Pulmonology' },
  { label: 'Radiology', value: 'Radiology' },
  { label: 'Registered Nurse', value: 'Registered Nurse' },
  { label: 'Rheumatology', value: 'Rheumatology' },
  { label: 'Trauma Surgery', value: 'Trauma Surgery' },
  { label: 'Urology', value: 'Urology' },
];

export default function RegisterScreen() {
  const [email, setEmail] = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { registerUser } = useAuth();
  const [professionalTitle, setProfessionalTitle] = useState('');
  const [npi, setNpi] = useState('');
  const [country, setCountry] = useState('');
  const [officeAddress, setOfficeAddress] = useState('');
  const [officeCity, setOfficeCity] = useState('');
  const [officeState, setOfficeState] = useState('');
  const [officeZip, setOfficeZip] = useState('');

  const handleRegister = async () => {
    setError('');
    
    // Required fields validation
    const requiredFields = {
      email,
      confirmEmail,
      firstName,
      lastName,
      specialty,
      phoneNumber,
      password,
      confirmPassword,
      professionalTitle,
      country,
      officeAddress,
      officeCity,
      officeZip,
    };

    // Check if USA is selected and state is required
    if (country === 'USA' && !officeState) {
      setError('State is required when USA is selected.');
      return;
    }

    // Check all required fields
    const missingFields = Object.entries(requiredFields)
      .filter(([key, value]) => !value)
      .map(([key]) => key);

    if (missingFields.length > 0) {
      setError(`Please fill in all required fields: ${missingFields.join(', ')}`);
      return;
    }

    if (email !== confirmEmail) {
      setError('Emails do not match.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }
    
    setIsLoading(true);
    try {
      // Prepare user data with all attributes
      const userData = {
        firstName,
        lastName,
        email,
        phoneNumber,
        npiNumber: npi,
        specialty,
        professionalTitle,
        country,
        officeAddress,
        officeCity,
        officeState: country === 'USA' ? officeState : '',
        officeZip,
        password, // Include password in the request
      };
      
      // Register user without email verification - go straight to document upload
      await registerUser(email, password, userData);
      
      // Navigate directly to credentials upload screen
      router.push('/auth/credentials');
    } catch (error: any) {
      console.error('Registration error details:', error);
      console.error('Error name:', error.name);
      console.error('Error message:', error.message);
      console.error('Error code:', error.code);
      
      if (error.name === 'UsernameExistsException') {
        setError('An account with this email already exists. Please sign in instead.');
      } else if (error.name === 'InvalidPasswordException') {
        setError('Password must be at least 8 characters and contain uppercase, lowercase, numbers, and special characters.');
      } else if (error.name === 'InvalidParameterException') {
        setError('Invalid email format or missing required fields.');
      } else {
        setError(`Registration failed: ${error.message || 'Unknown error'}`);
      }
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleBackToLogin = () => {
    router.replace('/auth/phone');
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.container}>
          <View style={styles.logoContainer}>
            <Image
              source={require('@/assets/images/SignoutSquareLogo.png')}
              style={styles.logo}
              resizeMode="contain"
            />
          </View>
          <Text style={styles.title}>Register for Signout</Text>
          <Text style={styles.subtitle}>
            Start your secure healthcare communication journey
          </Text>

          <View style={styles.securityInfo}>
            <Shield color="#0EA5E9" size={20} />
            <Text style={styles.securityText}>
              Encrypted, secure, and HIPAA-compliant
            </Text>
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.label}>Email Address *</Text>
            <TextInput
              style={styles.input}
              placeholder="you@email.com"
              keyboardType="email-address"
              autoCapitalize="none"
              value={email}
              onChangeText={setEmail}
            />
            <Text style={styles.label}>Confirm Email Address *</Text>
            <TextInput
              style={styles.input}
              placeholder="Confirm your email"
              keyboardType="email-address"
              autoCapitalize="none"
              value={confirmEmail}
              onChangeText={setConfirmEmail}
            />
            <Text style={styles.label}>First Name *</Text>
            <TextInput
              style={styles.input}
              placeholder="First Name"
              value={firstName}
              onChangeText={setFirstName}
            />
            <Text style={styles.label}>Last Name *</Text>
            <TextInput
              style={styles.input}
              placeholder="Last Name"
              value={lastName}
              onChangeText={setLastName}
            />
            <Text style={styles.label}>Professional Title *</Text>
            <CustomDropdown
              options={PROFESSIONAL_TITLES}
              value={professionalTitle}
              onValueChange={setProfessionalTitle}
              placeholder="Select title..."
              forceLightTheme={true}
            />
            <Text style={styles.label}>Specialty *</Text>
            <CustomDropdown
              options={SPECIALTIES}
              value={specialty}
              onValueChange={setSpecialty}
              placeholder="Select specialty..."
              forceLightTheme={true}
            />
            <Text style={styles.label}>Phone Number *</Text>
            <PhoneNumberInput
              value={phoneNumber}
              onChangeText={setPhoneNumber}
              placeholder="Enter phone number"
              forceLightTheme={true}
            />
            <Text style={styles.label}>Password *</Text>
            <TextInput
              style={styles.input}
              placeholder="Password (min 8 characters)"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
            <Text style={styles.label}>Confirm Password *</Text>
            <TextInput
              style={styles.input}
              placeholder="Confirm Password"
              secureTextEntry
              value={confirmPassword}
              onChangeText={setConfirmPassword}
            />
            <Text style={styles.label}>NPI Number (Optional)</Text>
            <TextInput
              style={styles.input}
              placeholder="10-digit NPI"
              keyboardType="number-pad"
              value={npi}
              onChangeText={setNpi}
              maxLength={10}
            />
            <Text style={styles.label}>Country *</Text>
            <CustomDropdown
              options={COUNTRIES}
              value={country}
              onValueChange={setCountry}
              placeholder="Select country..."
              forceLightTheme={true}
            />
            <Text style={styles.label}>Office Address *</Text>
            <TextInput
              style={styles.input}
              placeholder="123 Main St"
              value={officeAddress}
              onChangeText={setOfficeAddress}
            />
            <Text style={styles.label}>City *</Text>
            <TextInput
              style={styles.input}
              placeholder="City"
              value={officeCity}
              onChangeText={setOfficeCity}
            />
            <Text style={styles.label}>
              State {country === 'USA' ? '*' : '(Optional)'}
            </Text>
            {country === 'USA' ? (
              <CustomDropdown
                options={US_STATES}
                value={officeState}
                onValueChange={setOfficeState}
                placeholder="Select state..."
                forceLightTheme={true}
              />
            ) : (
              <TextInput
                style={styles.input}
                placeholder="State/Province"
                value={officeState}
                onChangeText={setOfficeState}
              />
            )}
            <Text style={styles.label}>Zip Code *</Text>
            <TextInput
              style={styles.input}
              placeholder="Zip Code"
              keyboardType="number-pad"
              value={officeZip}
              onChangeText={setOfficeZip}
              maxLength={10}
            />
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <TouchableOpacity
            style={[
              styles.continueButton,
              isLoading && styles.continueButtonLoading,
            ]}
            onPress={handleRegister}
            disabled={isLoading}
          >
            <Text style={styles.continueButtonText}>
              {isLoading ? 'Sending Verification...' : 'Next'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity onPress={handleBackToLogin} style={styles.registerCta}>
            <Text style={styles.registerText}>Already have an account? <Text style={styles.registerNow}>Back to login</Text></Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
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
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoContainer: {
    width: 120,
    height: 120,
    marginBottom: 32,
    borderRadius: 30,
    backgroundColor: 'rgba(14, 165, 233, 0.1)',
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
    color: '#0F172A',
    textAlign: 'center',
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    color: '#64748B',
    textAlign: 'center',
    marginBottom: 32,
  },
  securityInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F0F9FF',
    padding: 12,
    borderRadius: 8,
    marginBottom: 32,
    maxWidth: 320,
  },
  securityText: {
    marginLeft: 8,
    color: '#0EA5E9',
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
    color: '#0F172A',
    marginBottom: 8,
  },
  input: {
    padding: 14,
    fontSize: 16,
    color: '#0F172A',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    marginBottom: 16,
    backgroundColor: '#F8FAFC',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
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
  continueButtonLoading: {
    opacity: 0.7,
  },
  continueButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  error: {
    color: '#EF4444',
    fontSize: 15,
    marginBottom: 8,
    textAlign: 'center',
  },
  registerCta: {
    marginTop: 8,
  },
  registerText: {
    color: '#64748B',
    fontSize: 15,
    textAlign: 'center',
  },
  registerNow: {
    color: '#0EA5E9',
    fontWeight: '700',
  },
}); 