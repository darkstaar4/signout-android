import React, { useState, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, ScrollView, TextInput, Dimensions } from 'react-native';
// import { ChevronDown, ChevronUp, Search } from 'lucide-react-native'; // Install: npm install lucide-react-native
// import { useTheme } from '@/context/ThemeContext'; // Implement your theme context

export interface DropdownOption {
  label: string;
  value: string;
}

// Mock components - replace with your actual imports
const ChevronDown = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color, borderRadius: size/2 }} />
);

const ChevronUp = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color, borderRadius: size/2 }} />
);

const Search = ({ color, size }: { color: string; size: number }) => (
  <View style={{ width: size, height: size, backgroundColor: color, borderRadius: size/2 }} />
);

const useTheme = () => ({
  isDark: false, // Replace with your theme logic
});

interface CustomDropdownProps {
  options: DropdownOption[];
  value: string;
  onValueChange: (value: string) => void;
  placeholder?: string;
  searchable?: boolean;
  disabled?: boolean;
  error?: string;
  forceLightTheme?: boolean;
}

export default function CustomDropdown({
  options,
  value,
  onValueChange,
  placeholder = 'Select an option...',
  searchable = true,
  disabled = false,
  error,
  forceLightTheme = false,
}: CustomDropdownProps) {
  const { isDark } = useTheme();
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownPosition, setDropdownPosition] = useState({ x: 0, y: 0, width: 0, height: 0 });
  const dropdownRef = useRef<TouchableOpacity>(null);

  // Use light theme if forced or if no theme context is available
  const shouldUseLightTheme = forceLightTheme || !isDark;

  const filteredOptions = searchable
    ? options.filter(option =>
        option.label.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : options;

  const selectedOption = options.find(option => option.value === value);

  const handlePress = () => {
    if (disabled) return;
    
    if (!isOpen) {
      // Measure the dropdown position before opening
      dropdownRef.current?.measure((x, y, width, height, pageX, pageY) => {
        const screenHeight = Dimensions.get('window').height;
        const dropdownHeight = Math.min(300, filteredOptions.length * 50 + 100);
        
        // Check if dropdown should open upward
        const shouldOpenUpward = pageY + height + dropdownHeight > screenHeight;
        
        setDropdownPosition({
          x: pageX,
          y: shouldOpenUpward ? pageY - dropdownHeight : pageY + height,
          width: width,
          height: dropdownHeight,
        });
      });
    }
    
    setIsOpen(!isOpen);
    setSearchQuery('');
  };

  const handleOptionSelect = (optionValue: string) => {
    onValueChange(optionValue);
    setIsOpen(false);
    setSearchQuery('');
  };

  const handleClose = () => {
    setIsOpen(false);
    setSearchQuery('');
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        ref={dropdownRef}
        style={[
          styles.dropdown,
          !shouldUseLightTheme && styles.dropdownDark,
          error && styles.dropdownError,
          disabled && styles.dropdownDisabled,
          isOpen && styles.dropdownOpen,
        ]}
        onPress={handlePress}
        disabled={disabled}
      >
        <Text
          style={[
            styles.dropdownText,
            !shouldUseLightTheme && styles.dropdownTextDark,
            !selectedOption && styles.placeholder,
            disabled && styles.dropdownTextDisabled,
          ]}
          numberOfLines={1}
        >
          {selectedOption ? selectedOption.label : placeholder}
        </Text>
        
        <View style={styles.iconContainer}>
          {isOpen ? (
            <ChevronUp 
              color={shouldUseLightTheme ? '#64748B' : '#94A3B8'} 
              size={20} 
            />
          ) : (
            <ChevronDown 
              color={shouldUseLightTheme ? '#64748B' : '#94A3B8'} 
              size={20} 
            />
          )}
        </View>
      </TouchableOpacity>

      {error && (
        <Text style={styles.errorText}>{error}</Text>
      )}

      <Modal
        visible={isOpen}
        transparent={true}
        animationType="fade"
        onRequestClose={handleClose}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={handleClose}
        >
          <View
            style={[
              styles.modalContent,
              !shouldUseLightTheme && styles.modalContentDark,
              {
                position: 'absolute',
                left: dropdownPosition.x,
                top: dropdownPosition.y,
                width: dropdownPosition.width,
                maxHeight: dropdownPosition.height,
              },
            ]}
          >
            {searchable && (
              <View style={[
                styles.searchContainer,
                !shouldUseLightTheme && styles.searchContainerDark,
              ]}>
                <Search 
                  color={shouldUseLightTheme ? '#94A3B8' : '#64748B'} 
                  size={16} 
                />
                <TextInput
                  style={[
                    styles.searchInput,
                    !shouldUseLightTheme && styles.searchInputDark,
                  ]}
                  placeholder="Search..."
                  placeholderTextColor={shouldUseLightTheme ? '#94A3B8' : '#64748B'}
                  value={searchQuery}
                  onChangeText={setSearchQuery}
                  autoFocus
                />
              </View>
            )}
            
            <ScrollView style={styles.optionsList} showsVerticalScrollIndicator={false}>
              {filteredOptions.length === 0 ? (
                <View style={styles.noOptionsContainer}>
                  <Text style={[
                    styles.noOptionsText,
                    !shouldUseLightTheme && styles.noOptionsTextDark,
                  ]}>
                    No options found
                  </Text>
                </View>
              ) : (
                filteredOptions.map((option) => (
                  <TouchableOpacity
                    key={option.value}
                    style={[
                      styles.option,
                      !shouldUseLightTheme && styles.optionDark,
                      option.value === value && styles.selectedOption,
                      option.value === value && !shouldUseLightTheme && styles.selectedOptionDark,
                    ]}
                    onPress={() => handleOptionSelect(option.value)}
                  >
                    <Text
                      style={[
                        styles.optionText,
                        !shouldUseLightTheme && styles.optionTextDark,
                        option.value === value && styles.selectedOptionText,
                      ]}
                      numberOfLines={1}
                    >
                      {option.label}
                    </Text>
                  </TouchableOpacity>
                ))
              )}
            </ScrollView>
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
  dropdown: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    backgroundColor: '#F8FAFC',
    minHeight: 48,
  },
  dropdownDark: {
    borderColor: '#374151',
    backgroundColor: '#1F2937',
  },
  dropdownError: {
    borderColor: '#EF4444',
  },
  dropdownDisabled: {
    opacity: 0.5,
  },
  dropdownOpen: {
    borderColor: '#0EA5E9',
  },
  dropdownText: {
    flex: 1,
    fontSize: 16,
    color: '#0F172A',
  },
  dropdownTextDark: {
    color: '#F1F5F9',
  },
  dropdownTextDisabled: {
    color: '#94A3B8',
  },
  placeholder: {
    color: '#94A3B8',
  },
  iconContainer: {
    marginLeft: 8,
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
  modalContent: {
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 8,
  },
  modalContentDark: {
    backgroundColor: '#1F2937',
    borderColor: '#374151',
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E2E8F0',
  },
  searchContainerDark: {
    borderBottomColor: '#374151',
  },
  searchInput: {
    flex: 1,
    marginLeft: 8,
    fontSize: 16,
    color: '#0F172A',
    padding: 0,
  },
  searchInputDark: {
    color: '#F1F5F9',
  },
  optionsList: {
    maxHeight: 200,
  },
  option: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  optionDark: {
    borderBottomColor: '#374151',
  },
  selectedOption: {
    backgroundColor: '#EFF6FF',
  },
  selectedOptionDark: {
    backgroundColor: '#1E3A8A',
  },
  optionText: {
    fontSize: 16,
    color: '#0F172A',
  },
  optionTextDark: {
    color: '#F1F5F9',
  },
  selectedOptionText: {
    color: '#0EA5E9',
    fontWeight: '600',
  },
  noOptionsContainer: {
    padding: 16,
    alignItems: 'center',
  },
  noOptionsText: {
    fontSize: 16,
    color: '#94A3B8',
  },
  noOptionsTextDark: {
    color: '#6B7280',
  },
}); 