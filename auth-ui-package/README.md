# Authentication UI Package

This package contains clean, reusable authentication UI components extracted from a healthcare messaging app. All components are designed to be easily integrated into your existing React Native project.

## 📦 What's Included

### Screens
- **LoginScreen.tsx** - Login form with email/password and forgot password modal
- **RegisterScreen.tsx** - Registration form with healthcare professional fields
- **CredentialsScreen.tsx** - Document upload screen for verification
- **PendingScreen.tsx** - Waiting screen for account approval
- **SplashScreen.tsx** - App loading/splash screen

### Components
- **PhoneNumberInput.tsx** - Custom phone number input with country codes
- **CustomDropdown.tsx** - Custom dropdown component
- **USAPrideBanner.tsx** - Optional banner component

### Assets
- **SignoutSquareLogo.png** - Logo (replace with your own)
- **splash.png** - Splash screen image (replace with your own)

### Types & Interfaces
- **AuthContextInterface.tsx** - TypeScript interfaces for AuthContext
- **user.ts** - User type definitions

## 🚀 Quick Setup

### 1. Install Dependencies

```bash
npm install lucide-react-native
npm install expo-document-picker expo-image-picker  # For file uploads
npm install @react-native-async-storage/async-storage  # For local storage
```

### 2. Copy Files to Your Project

Copy the files to your project structure:
```
your-project/
├── screens/
│   ├── LoginScreen.tsx
│   ├── RegisterScreen.tsx
│   ├── CredentialsScreen.tsx
│   ├── PendingScreen.tsx
│   └── SplashScreen.tsx
├── components/
│   ├── PhoneNumberInput.tsx
│   ├── CustomDropdown.tsx
│   └── USAPrideBanner.tsx
├── assets/
│   ├── SignoutSquareLogo.png
│   └── splash.png
└── types/
    └── user.ts
```

### 3. Implement Required Contexts

You need to implement these contexts in your project:

#### AuthContext
```tsx
// context/AuthContext.tsx
import React, { createContext, useContext } from 'react';
import { AuthContextType } from '../types/AuthContextInterface';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Your authentication logic here
  const authValue: AuthContextType = {
    status: 'unauthenticated',
    session: null,
    user: null,
    isLoading: false,
    
    login: async (email: string, password: string) => {
      // Implement your login logic
    },
    
    registerUser: async (email: string, password: string, userData: any) => {
      // Implement your registration logic
    },
    
    uploadCredentials: async (credentials: Record<string, any>) => {
      // Implement your credential upload logic
    },
    
    signOut: async () => {
      // Implement your sign out logic
    },
  };

  return (
    <AuthContext.Provider value={authValue}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

#### ThemeContext (Optional)
```tsx
// context/ThemeContext.tsx
import React, { createContext, useContext, useState } from 'react';

interface ThemeContextType {
  isDark: boolean;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [isDark, setIsDark] = useState(false);

  const toggleTheme = () => setIsDark(!isDark);

  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
```

### 4. Update Navigation

Replace the placeholder navigation with your routing solution:

```tsx
// In LoginScreen.tsx, replace:
const router = {
  replace: (path: string) => console.log('Navigate to:', path),
  push: (path: string) => console.log('Navigate to:', path),
};

// With your navigation solution, e.g., React Navigation:
import { useNavigation } from '@react-navigation/native';

const navigation = useNavigation();
const router = {
  replace: (path: string) => navigation.replace(path),
  push: (path: string) => navigation.navigate(path),
};
```

### 5. Update Asset Paths

Update image imports to match your project structure:

```tsx
// Replace:
source={require('../assets/SignoutSquareLogo.png')}

// With your asset path:
source={require('./assets/your-logo.png')}
```

## 🎨 Customization

### Colors & Branding
The components use a consistent color scheme. Update these in the StyleSheet:

- Primary: `#0EA5E9` (blue)
- Secondary: `#2C5282` (darker blue)
- Text: `#0F172A` (dark)
- Background: `#FFFFFF` (white)
- Error: `#EF4444` (red)

### Text & Labels
Update text content in each component:
- App name in LoginScreen
- Field labels in RegisterScreen
- Instructions and help text

### Form Fields
The RegisterScreen includes healthcare-specific fields:
- NPI Number
- Professional Title
- Specialty
- Office Address
- Country/State selection

Remove or modify these based on your needs.

## 🔧 Component Features

### LoginScreen
- Email/password login
- Forgot password modal
- Reset password flow
- Form validation
- Loading states

### RegisterScreen
- Multi-step registration form
- Phone number input with country codes
- Dropdown selections
- Form validation
- Professional fields for healthcare

### CredentialsScreen
- Document upload (camera/gallery)
- File type validation
- Upload progress
- Security messaging

### PhoneNumberInput
- Country code selection
- Phone number formatting
- E.164 format output
- Theme support

### CustomDropdown
- Modal-based dropdown
- Search functionality
- Theme support
- Keyboard-friendly

## 📱 Platform Support

- ✅ iOS
- ✅ Android
- ✅ Light/Dark theme support
- ✅ Keyboard handling
- ✅ Accessibility ready

## 🔗 Integration Tips

1. **Start with LoginScreen** - It's the simplest to integrate
2. **Implement AuthContext gradually** - Start with basic login/logout
3. **Customize styling** - Update colors and fonts to match your brand
4. **Test on both platforms** - Some styling may need platform-specific adjustments
5. **Add your API endpoints** - Replace placeholder URLs with your backend

## 📝 TODO for Integration

- [ ] Implement AuthContext with your authentication logic
- [ ] Set up navigation routing
- [ ] Replace placeholder API endpoints
- [ ] Update branding and colors
- [ ] Replace logo and assets
- [ ] Test on your target platforms
- [ ] Add error handling for your specific use cases

## 💡 Notes

- All components are functional and ready to use
- No external authentication dependencies included
- Styling is responsive and mobile-optimized
- Form validation is included
- Loading states and error handling are built-in

Need help with integration? Check the TODO comments in each component file for specific implementation guidance. 