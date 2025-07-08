import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform, ScrollView, Modal, Dimensions } from 'react-native';
import { ChevronDown, ChevronUp } from 'lucide-react-native';

export type DropdownOption = {
  label: string;
  value: string;
};

type CustomDropdownProps = {
  value: string;
  onValueChange: (value: string) => void;
  options: DropdownOption[];
  placeholder?: string;
  editable?: boolean;
  error?: string;
  forceLightTheme?: boolean;
};

export default function CustomDropdown({
  value,
  onValueChange,
  options,
  placeholder = 'Select an option...',
  editable = true,
  error,
  forceLightTheme = false
}: CustomDropdownProps) {
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

  const [showDropdown, setShowDropdown] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState({ x: 0, y: 0, width: 0 });
  const dropdownRef = useRef<TouchableOpacity>(null);

  // Use light theme if forced or if no theme context is available
  const shouldUseLightTheme = forceLightTheme || !isDark;

  const toggleDropdown = () => {
    if (editable) {
      if (!showDropdown) {
        // Measure the position of the dropdown button
        dropdownRef.current?.measure((x, y, width, height, pageX, pageY) => {
          setDropdownPosition({
            x: pageX,
            y: pageY + height,
            width: width
          });
        });
      }
      setShowDropdown(!showDropdown);
    }
  };

  const handleOptionSelect = (optionValue: string) => {
    onValueChange(optionValue);
    setShowDropdown(false);
  };

  const getDisplayValue = (): string => {
    if (!value) return placeholder;
    const selectedOption = options.find(option => option.value === value);
    return selectedOption ? selectedOption.label : placeholder;
  };

  return (
    <View style={styles.container}>
      <View style={[
        styles.dropdownContainer,
        !shouldUseLightTheme && styles.dropdownContainerDark,
        error ? styles.dropdownContainerError : undefined,
        !editable && styles.dropdownContainerDisabled
      ]}>
        <TouchableOpacity
          ref={dropdownRef}
          style={[
            styles.dropdownButton,
            !shouldUseLightTheme && styles.dropdownButtonDark,
            !editable && styles.dropdownButtonDisabled,
            showDropdown && styles.dropdownButtonActive
          ]}
          onPress={toggleDropdown}
          disabled={!editable}
        >
          <Text style={[
            styles.dropdownText,
            !shouldUseLightTheme && styles.dropdownTextDark,
            !editable && styles.dropdownTextDisabled,
            !value && styles.placeholderText
          ]}>
            {getDisplayValue()}
          </Text>
          {showDropdown ? (
            <ChevronUp size={16} color={shouldUseLightTheme ? '#666666' : '#64748B'} />
          ) : (
            <ChevronDown size={16} color={shouldUseLightTheme ? '#666666' : '#64748B'} />
          )}
        </TouchableOpacity>
      </View>

      {/* Modal Dropdown */}
      <Modal
        visible={showDropdown}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowDropdown(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowDropdown(false)}
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
              {options.map((option) => (
                <TouchableOpacity
                  key={option.value}
                  style={[
                    styles.modalDropdownItem,
                    value === option.value && styles.modalDropdownItemSelected,
                    !shouldUseLightTheme && styles.modalDropdownItemDark,
                    value === option.value && !shouldUseLightTheme && styles.modalDropdownItemSelectedDark
                  ]}
                  onPress={() => handleOptionSelect(option.value)}
                >
                  <Text style={[
                    styles.modalDropdownItemText,
                    value === option.value && styles.modalDropdownItemTextSelected,
                    !shouldUseLightTheme && styles.modalDropdownItemTextDark,
                    value === option.value && !shouldUseLightTheme && styles.modalDropdownItemTextSelectedDark
                  ]}>
                    {option.label}
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
  dropdownContainer: {
    position: 'relative',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    backgroundColor: '#F8FAFC',
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  dropdownContainerDark: {
    borderColor: '#334155',
    backgroundColor: '#1E293B',
  },
  dropdownContainerError: {
    borderColor: '#EF4444',
  },
  dropdownContainerDisabled: {
    opacity: 0.5,
  },
  dropdownButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingVertical: 14,
    backgroundColor: 'transparent',
  },
  dropdownButtonDark: {
    backgroundColor: 'transparent',
  },
  dropdownButtonDisabled: {
    opacity: 0.5,
  },
  dropdownButtonActive: {
    backgroundColor: '#E2E8F0',
  },
  dropdownText: {
    fontSize: 16,
    color: '#000000',
    flex: 1,
  },
  dropdownTextDark: {
    color: '#F8FAFC',
  },
  dropdownTextDisabled: {
    color: '#94A3B8',
  },
  placeholderText: {
    color: '#666666',
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
  modalDropdownItemText: {
    fontSize: 16,
    color: '#000000',
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