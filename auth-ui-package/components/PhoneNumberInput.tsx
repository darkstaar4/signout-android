import React, { useState, useEffect, useRef } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Platform, ScrollView, Modal } from 'react-native';
import { ChevronDown, ChevronUp } from 'lucide-react-native';

export type CountryCode = {
  code: string;
  name: string;
  dialCode: string;
  flag: string;
};

const COUNTRY_CODES: CountryCode[] = [
  { code: 'US', name: 'USA', dialCode: '+1', flag: '🇺🇸' },
  { code: 'CA', name: 'Canada', dialCode: '+1', flag: '🇨🇦' },
  { code: 'MX', name: 'Mexico', dialCode: '+52', flag: '🇲🇽' },
];

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
}: PhoneNumberInputProps & { forceLightTheme?: boolean }) {
  // Try to get theme context, but default to light theme if not available
  let isDark = false;
  try {
    const { useTheme } = require('@/context/ThemeContext');
    const theme = useTheme();
    isDark = theme?.isDark || false;
  } catch (e) {
    // Theme context not available, default to light
    isDark = false;
  }

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
      default:
        return digits;
    }
  };

  const handlePhoneNumberChange = (text: string) => {
    const formatted = formatPhoneNumber(text, selectedCountry.code);
    setPhoneNumber(formatted);
    
    // Create the full E.164 format for Cognito
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

  const getDisplayValue = (): string => {
    if (!phoneNumber) return '';
    return `${selectedCountry.dialCode} ${phoneNumber}`;
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
            <Text style={[
              styles.countryCode,
              !shouldUseLightTheme && styles.countryCodeDark,
              !editable && styles.countryCodeDisabled
            ]}>
              {selectedCountry.dialCode}
            </Text>
            {showCountryPicker ? (
              <ChevronUp size={16} color={shouldUseLightTheme ? '#666666' : '#64748B'} />
            ) : (
              <ChevronDown size={16} color={shouldUseLightTheme ? '#666666' : '#64748B'} />
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
          placeholderTextColor={shouldUseLightTheme ? '#666666' : '#64748B'}
          editable={editable}
          keyboardType="phone-pad"
          maxLength={15}
        />
      </View>

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
            <ScrollView style={styles.modalScrollView}>
              {COUNTRY_CODES.map((country) => (
                <TouchableOpacity
                  key={country.code}
                  style={[
                    styles.modalDropdownItem,
                    selectedCountry.code === country.code && styles.modalDropdownItemSelected,
                    !shouldUseLightTheme && styles.modalDropdownItemDark,
                    selectedCountry.code === country.code && !shouldUseLightTheme && styles.modalDropdownItemSelectedDark
                  ]}
                  onPress={() => handleCountryChange(country.code)}
                >
                  <Text style={styles.modalDropdownItemFlag}>
                    {country.flag}
                  </Text>
                  <Text style={[
                    styles.modalDropdownItemText,
                    selectedCountry.code === country.code && styles.modalDropdownItemTextSelected,
                    !shouldUseLightTheme && styles.modalDropdownItemTextDark,
                    selectedCountry.code === country.code && !shouldUseLightTheme && styles.modalDropdownItemTextSelectedDark
                  ]}>
                    {country.name} ({country.dialCode})
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Error Message */}
      {error && (
        <Text style={styles.errorText}>{error}</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
    position: 'relative',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    backgroundColor: '#F8FAFC',
    marginBottom: 16,
  },
  inputContainerDark: {
    borderColor: '#334155',
    backgroundColor: '#1E293B',
  },
  inputContainerError: {
    borderColor: '#EF4444',
  },
  countryPickerContainer: {
    position: 'relative',
    flexDirection: 'row',
    alignItems: 'center',
    borderRightWidth: 1,
    borderRightColor: '#E2E8F0',
    backgroundColor: '#F1F5F9',
    borderTopLeftRadius: 8,
    borderBottomLeftRadius: 8,
  },
  countryPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderRightWidth: 1,
    borderRightColor: '#E2E8F0',
    backgroundColor: '#F1F5F9',
    borderTopLeftRadius: 8,
    borderBottomLeftRadius: 8,
  },
  countryPickerDark: {
    borderRightColor: '#334155',
    backgroundColor: '#0F172A',
  },
  countryPickerDisabled: {
    opacity: 0.5,
  },
  countryPickerActive: {
    backgroundColor: '#E2E8F0',
  },
  countryCode: {
    fontSize: 16,
    fontWeight: '600',
    color: '#000000',
    marginRight: 4,
  },
  countryCodeDark: {
    color: '#F8FAFC',
  },
  countryCodeDisabled: {
    color: '#94A3B8',
  },
  phoneInput: {
    flex: 1,
    fontSize: 16,
    color: '#000000',
    paddingHorizontal: 12,
    paddingVertical: 12,
    backgroundColor: 'transparent',
  },
  phoneInputDark: {
    color: '#F8FAFC',
  },
  phoneInputDisabled: {
    color: '#94A3B8',
  },
  errorText: {
    fontSize: 12,
    color: '#EF4444',
    marginTop: 4,
    marginLeft: 4,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  modalDropdown: {
    position: 'absolute',
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    padding: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 8,
    maxHeight: 300,
    minWidth: 200,
  },
  modalDropdownDark: {
    backgroundColor: '#1E293B',
  },
  modalScrollView: {
    maxHeight: 284, // 300 - 16 (padding)
  },
  modalDropdownItem: {
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 6,
  },
  modalDropdownItemSelected: {
    backgroundColor: '#E2E8F0',
  },
  modalDropdownItemDark: {
    backgroundColor: '#334155',
  },
  modalDropdownItemSelectedDark: {
    backgroundColor: '#475569',
  },
  modalDropdownItemFlag: {
    fontSize: 20,
    marginRight: 12,
  },
  modalDropdownItemText: {
    fontSize: 16,
    color: '#000000',
    flex: 1,
  },
  modalDropdownItemTextSelected: {
    fontWeight: '600',
  },
  modalDropdownItemTextDark: {
    color: '#F8FAFC',
  },
  modalDropdownItemTextSelectedDark: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
});