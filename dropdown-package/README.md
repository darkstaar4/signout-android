# Dropdown Components Package

A comprehensive set of dropdown components for React Native applications, specifically designed for healthcare and professional registration forms.

## 📦 What's Included

### Components
- **CustomDropdown.tsx** - Fully customizable dropdown with search functionality
- **PhoneNumberInput.tsx** - Phone number input with country code selection

### Data
- **dropdownData.ts** - Comprehensive dropdown data including:
  - **Professional Titles** (15 options: MD, DO, PA-C, NP, RN, etc.)
  - **Medical Specialties** (50+ options: Cardiology, Emergency Medicine, etc.)
  - **Countries** (16 countries with flag emojis)
  - **US States** (All 50 states)
  - **Canadian Provinces** (All 13 provinces/territories)
  - **Country Codes** (16 countries with dial codes and flags)
  - **Helper Functions** for dynamic state/province selection

## 🚀 Quick Integration

### 1. Install Dependencies
```bash
npm install lucide-react-native
```

### 2. Copy Files
Copy the entire `dropdown-package` folder to your project:
```
your-project/
├── components/
│   ├── CustomDropdown.tsx
│   └── PhoneNumberInput.tsx
├── data/
│   └── dropdownData.ts
```

### 3. Basic Usage

#### CustomDropdown
```tsx
import CustomDropdown from './components/CustomDropdown';
import { MEDICAL_SPECIALTIES, COUNTRIES, US_STATES } from './data/dropdownData';

function RegistrationForm() {
  const [specialty, setSpecialty] = useState('');
  const [country, setCountry] = useState('');
  const [state, setState] = useState('');

  return (
    <View>
      <CustomDropdown
        options={MEDICAL_SPECIALTIES}
        value={specialty}
        onValueChange={setSpecialty}
        placeholder="Select specialty..."
        searchable={true}
      />
      
      <CustomDropdown
        options={COUNTRIES}
        value={country}
        onValueChange={setCountry}
        placeholder="Select country..."
      />
      
      {country === 'USA' && (
        <CustomDropdown
          options={US_STATES}
          value={state}
          onValueChange={setState}
          placeholder="Select state..."
        />
      )}
    </View>
  );
}
```

#### PhoneNumberInput
```tsx
import PhoneNumberInput from './components/PhoneNumberInput';

function ContactForm() {
  const [phoneNumber, setPhoneNumber] = useState('');

  return (
    <PhoneNumberInput
      value={phoneNumber}
      onChangeText={setPhoneNumber}
      placeholder="Enter phone number"
    />
  );
}
```

## 🎨 Component Features

### CustomDropdown
- ✅ **Searchable** - Built-in search functionality
- ✅ **Modal-based** - Smooth modal overlay interface
- ✅ **Keyboard-friendly** - Proper keyboard handling
- ✅ **Theme support** - Light/dark theme compatible
- ✅ **Error handling** - Error state styling
- ✅ **Accessibility** - Screen reader compatible
- ✅ **Responsive** - Adapts to screen size
- ✅ **Customizable** - Easy to style and modify

### PhoneNumberInput
- ✅ **Country code selection** - 16 countries supported
- ✅ **Auto-formatting** - Formats numbers by country
- ✅ **E.164 output** - International standard format
- ✅ **Flag display** - Visual country identification
- ✅ **Theme support** - Light/dark theme compatible
- ✅ **Validation ready** - Error state support

## 📊 Data Sets

### Professional Titles (15 options)
```typescript
MD, DO, PA-C, NP, RN, LPN, CNA, PharmD, DDS, DMD, DPT, OTR, SLP, Student, Other
```

### Medical Specialties (50+ options)
```typescript
Cardiology, Emergency Medicine, Family Medicine, Internal Medicine, Pediatrics, 
Surgery, Psychiatry, Radiology, Anesthesiology, Dermatology, Neurology, 
Oncology, Orthopedics, Ophthalmology, Urology, and many more...
```

### Countries (16 countries)
```typescript
🇺🇸 United States, 🇨🇦 Canada, 🇲🇽 Mexico, 🇬🇧 United Kingdom, 
🇦🇺 Australia, 🇩🇪 Germany, 🇫🇷 France, 🇮🇹 Italy, and more...
```

### Phone Number Support
```typescript
+1 (US/Canada), +44 (UK), +49 (Germany), +33 (France), +39 (Italy), 
+34 (Spain), +31 (Netherlands), +55 (Brazil), +52 (Mexico), and more...
```

## 🔧 Customization

### Adding Your Own Data
Edit `data/dropdownData.ts` to customize any dropdown options:

```typescript
// Add your own specialties
export const MEDICAL_SPECIALTIES = [
  { label: 'Your Custom Specialty', value: 'custom_specialty' },
  // ... existing options
];

// Add your own countries
export const COUNTRIES = [
  { label: '🇾🇴 Your Country', value: 'YOUR_COUNTRY' },
  // ... existing options
];

// Add your own country codes
export const COUNTRY_CODES = [
  { code: 'YC', name: 'Your Country', dialCode: '+999', flag: '🇾🇴' },
  // ... existing options
];
```

### Dynamic State/Province Selection
Use the helper functions for dynamic state selection:

```tsx
import { getStatesForCountry, countryRequiresState } from './data/dropdownData';

function AddressForm() {
  const [country, setCountry] = useState('');
  const [state, setState] = useState('');
  
  const stateOptions = getStatesForCountry(country);
  const requiresState = countryRequiresState(country);

  return (
    <View>
      <CustomDropdown
        options={COUNTRIES}
        value={country}
        onValueChange={(value) => {
          setCountry(value);
          setState(''); // Reset state when country changes
        }}
        placeholder="Select country..."
      />
      
      {requiresState && (
        <CustomDropdown
          options={stateOptions}
          value={state}
          onValueChange={setState}
          placeholder="Select state/province..."
        />
      )}
    </View>
  );
}
```

### Styling Customization
Both components use StyleSheet for easy customization:

```typescript
// Customize colors
const styles = StyleSheet.create({
  dropdown: {
    borderColor: '#YOUR_COLOR',
    backgroundColor: '#YOUR_BACKGROUND',
  },
  // ... other styles
});
```

## 🔌 Implementation Notes

### Required Implementations
Replace the mock implementations with your actual:

1. **Icon Components** - Replace mock icons with lucide-react-native:
```tsx
import { ChevronDown, ChevronUp, Search } from 'lucide-react-native';
```

2. **Theme Context** - Implement your theme logic:
```tsx
import { useTheme } from '@/context/ThemeContext';
```

### Optional Features
- **Multi-language support** - Translate dropdown labels
- **Custom validation** - Add validation rules
- **Additional formatting** - Phone number formatting for more countries
- **Accessibility** - Add accessibility labels

## 📱 Platform Support
- ✅ **iOS** - Full support with native feel
- ✅ **Android** - Material Design compatible
- ✅ **Light/Dark themes** - Automatic theme detection
- ✅ **Keyboard handling** - Proper keyboard behavior
- ✅ **Screen sizes** - Responsive design

## 🎯 Use Cases

Perfect for:
- **Healthcare apps** - Professional registration forms
- **Business apps** - Employee registration
- **International apps** - Multi-country support
- **Professional services** - Credential collection
- **E-commerce** - Address forms with proper validation

## 📄 License
MIT License - Feel free to use in your projects!

## 🤝 Contributing
This is an extracted component package. Customize as needed for your specific use case! 