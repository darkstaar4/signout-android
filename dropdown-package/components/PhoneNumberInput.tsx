import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, Modal } from 'react-native';
// import { ChevronDown, ChevronUp } from 'lucide-react-native'; // Install: npm install lucide-react-native
// import { useTheme } from '@/context/ThemeContext'; // Implement your theme context

// Import country codes data
import { COUNTRY_CODES, type CountryCode } from '../data/dropdownData';

// Mock components - replace with your actual imports
const ChevronDown = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color, borderRadius: size/2 }} />
);

const ChevronUp = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color, borderRadius: size/2 }} />
);

const useTheme = () => ({
  isDark: false, // Replace with your theme logic
});

type PhoneNumberInputProps = {
  value: string;
  onChangeText: (value: string) => void;
  placeholder?: string;
  editable?: boolean;
  error?: string;
  forceLightTheme?: boolean;
};

export default function PhoneNumberInput({
  value,
  onChangeText,
  placeholder = 'Enter phone number',
  editable = true,
  error,
  forceLightTheme = false
}: PhoneNumberInputProps) {
  const { isDark } = useTheme();
  const [showCountryPicker, setShowCountryPicker] = useState(false);
  const [selectedCountry, setSelectedCountry] = useState<CountryCode>(COUNTRY_CODES[0]);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [dropdownPosition, setDropdownPosition] = useState({ x: 0, y: 0, width: 0 });
  const dropdownRef = useRef<TouchableOpacity>(null);

  // Use light theme if forced or if no theme context is available
  const shouldUseLightTheme = forceLightTheme || !isDark;

  // Parse the full phone number on component mount
  useEffect(() => {
    if (value) {
      parsePhoneNumber(value);
    }
  }, [value]);

  const parsePhoneNumber = (fullNumber: string) => {
    // Remove any non-digit characters except +
    const cleanNumber = fullNumber.replace(/[^\d+]/g, '');
    
    // Find matching country code
    const country = COUNTRY_CODES.find(c => cleanNumber.startsWith(c.dialCode));
    if (country) {
      setSelectedCountry(country);
      // Extract the phone number part (remove country code)
      const numberPart = cleanNumber.substring(country.dialCode.length);
      setPhoneNumber(formatPhoneNumber(numberPart, country.code));
    } else {
      // Default to US if no country code found
      setSelectedCountry(COUNTRY_CODES[0]);
      setPhoneNumber(formatPhoneNumber(cleanNumber, 'US'));
    }
  };

  const formatPhoneNumber = (number: string, countryCode: string): string => {
    // Remove all non-digits
    const digits = number.replace(/\D/g, '');
    
    switch (countryCode) {
      case 'US':
      case 'CA':
        // Format as (XXX) XXX-XXXX
        if (digits.length <= 3) return digits;
        if (digits.length <= 6) return `(${digits.slice(0, 3)}) ${digits.slice(3)}`;
        return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6, 10)}`;
      case 'MX':
        // Format as XXX XXX XXXX
        if (digits.length <= 3) return digits;
        if (digits.length <= 6) return `${digits.slice(0, 3)} ${digits.slice(3)}`;
        if (digits.length <= 9) return `${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6)}`;
        return `${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6, 10)}`;
      case 'GB':
        // Format as XXXXX XXXXXX
        if (digits.length <= 5) return digits;
        return `${digits.slice(0, 5)} ${digits.slice(5, 11)}`;
      default:
        // Default formatting for other countries
        return digits;
    }
  };

  const handlePhoneNumberChange = (text: string) => {
    const formatted = formatPhoneNumber(text, selectedCountry.code);
    setPhoneNumber(formatted);
    
    // Create the full E.164 format
    const digits = formatted.replace(/\D/g, '');
    const fullNumber = `${selectedCountry.dialCode}${digits}`;
    onChangeText(fullNumber);
  };

  const handleCountryChange = (countryCode: string) => {
    const country = COUNTRY_CODES.find(c => c.code === countryCode);
    if (country) {
      setSelectedCountry(country);
      // Reformat the phone number for the new country
      const digits = phoneNumber.replace(/\D/g, '');
      const formatted = formatPhoneNumber(digits, country.code);
      setPhoneNumber(formatted);
      
      // Update the full number
      const fullNumber = `${country.dialCode}${digits}`;
      onChangeText(fullNumber);
    }
    setShowCountryPicker(false);
  };

  const toggleCountryPicker = () => {
    if (editable) {
      if (!showCountryPicker) {
        // Measure the position of the dropdown button
        dropdownRef.current?.measure((x, y, width, height, pageX, pageY) => {
          setDropdownPosition({
            x: pageX,
            y: pageY + height,
            width: width
          });
        });
      }
      setShowCountryPicker(!showCountryPicker);
    }
  };

  return (
    <View style={styles.container}>
      <View style={[
        styles.inputContainer,
        !shouldUseLightTheme && styles.inputContainerDark,
        error ? styles.inputContainerError : undefined
      ]}>
        {/* Country Code Picker */}
        <View style={styles.countryPickerContainer}>
          <TouchableOpacity
            ref={dropdownRef}
            style={[
              styles.countryPicker,
              !shouldUseLightTheme && styles.countryPickerDark,
              !editable && styles.countryPickerDisabled,
              showCountryPicker && styles.countryPickerActive
            ]}
            onPress={toggleCountryPicker}
            disabled={!editable}
          >
            <Text style={styles.countryFlag}>
              {selectedCountry.flag}
            </Text>
            <Text style={[
              styles.countryCode,
              !shouldUseLightTheme && styles.countryCodeDark,
              !editable && styles.countryCodeDisabled
            ]}>
              {selectedCountry.dialCode}
            </Text>
            {showCountryPicker ? (
              <ChevronUp color={shouldUseLightTheme ? '#94A3B8' : '#64748B'} size={16} />
            ) : (
              <ChevronDown color={shouldUseLightTheme ? '#94A3B8' : '#64748B'} size={16} />
            )}
          </TouchableOpacity>
        </View>

        {/* Phone Number Input */}
        <TextInput
          style={[
            styles.phoneInput,
            !shouldUseLightTheme && styles.phoneInputDark,
            !editable && styles.phoneInputDisabled
          ]}
          value={phoneNumber}
          onChangeText={handlePhoneNumberChange}
          placeholder={placeholder}
          placeholderTextColor={shouldUseLightTheme ? '#94A3B8' : '#64748B'}
          editable={editable}
          keyboardType="phone-pad"
          maxLength={20}
        />
      </View>

      {error && (
        <Text style={styles.errorText}>{error}</Text>
      )}

      {/* Modal Dropdown */}
      <Modal
        visible={showCountryPicker}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowCountryPicker(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowCountryPicker(false)}
        >
          <View style={[
            styles.modalDropdown,
            {
              left: dropdownPosition.x,
              top: dropdownPosition.y,
              width: dropdownPosition.width || 200,
            },
            !shouldUseLightTheme && styles.modalDropdownDark
          ]}>
            {COUNTRY_CODES.map((country) => (
              <TouchableOpacity
                key={country.code}
                style={[
                  styles.countryOption,
                  selectedCountry.code === country.code && styles.selectedCountryOption,
                  !shouldUseLightTheme && styles.countryOptionDark,
                  selectedCountry.code === country.code && !shouldUseLightTheme && styles.selectedCountryOptionDark
                ]}
                onPress={() => handleCountryChange(country.code)}
              >
                <Text style={styles.countryFlag}>
                  {country.flag}
                </Text>
                <Text style={[
                  styles.countryName,
                  !shouldUseLightTheme && styles.countryNameDark,
                  selectedCountry.code === country.code && styles.selectedCountryName
                ]}>
                  {country.name}
                </Text>
                <Text style={[
                  styles.countryDialCode,
                  !shouldUseLightTheme && styles.countryDialCodeDark,
                  selectedCountry.code === country.code && styles.selectedCountryDialCode
                ]}>
                  {country.dialCode}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    backgroundColor: '#F8FAFC',
    overflow: 'hidden',
  },
  inputContainerDark: {
    borderColor: '#374151',
    backgroundColor: '#1F2937',
  },
  inputContainerError: {
    borderColor: '#EF4444',
  },
  countryPickerContainer: {
    borderRightWidth: 1,
    borderRightColor: '#E2E8F0',
  },
  countryPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 14,
    minWidth: 120,
  },
  countryPickerDark: {
    borderRightColor: '#374151',
  },
  countryPickerDisabled: {
    opacity: 0.5,
  },
  countryPickerActive: {
    backgroundColor: '#E2E8F0',
  },
  countryFlag: {
    fontSize: 18,
    marginRight: 8,
  },
  countryCode: {
    fontSize: 16,
    color: '#0F172A',
    flex: 1,
  },
  countryCodeDark: {
    color: '#F1F5F9',
  },
  countryCodeDisabled: {
    color: '#94A3B8',
  },
  phoneInput: {
    flex: 1,
    paddingHorizontal: 14,
    paddingVertical: 14,
    fontSize: 16,
    color: '#0F172A',
  },
  phoneInputDark: {
    color: '#F1F5F9',
  },
  phoneInputDisabled: {
    color: '#94A3B8',
  },
  errorText: {
    color: '#EF4444',
    fontSize: 14,
    marginTop: 4,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalDropdown: {
    position: 'absolute',
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 8,
    maxHeight: 300,
  },
  modalDropdownDark: {
    backgroundColor: '#1F2937',
  },
  countryOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  countryOptionDark: {
    borderBottomColor: '#374151',
  },
  selectedCountryOption: {
    backgroundColor: '#EFF6FF',
  },
  selectedCountryOptionDark: {
    backgroundColor: '#1E3A8A',
  },
  countryName: {
    flex: 1,
    fontSize: 16,
    color: '#0F172A',
    marginLeft: 8,
  },
  countryNameDark: {
    color: '#F1F5F9',
  },
  selectedCountryName: {
    color: '#0EA5E9',
    fontWeight: '600',
  },
  countryDialCode: {
    fontSize: 16,
    color: '#64748B',
  },
  countryDialCodeDark: {
    color: '#94A3B8',
  },
  selectedCountryDialCode: {
    color: '#0EA5E9',
    fontWeight: '600',
  },
});