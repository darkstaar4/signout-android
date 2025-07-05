import React from 'react';

// User type interface
export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  npiNumber?: string;
  professionalTitle?: string;
  specialty?: string;
  officeAddress?: string;
  officeCity?: string;
  officeState?: string;
  officeZip?: string;
  country?: string;
  role?: 'user' | 'admin' | 'superadmin';
  isApproved?: boolean;
  isDisabled?: boolean;
  isDeleted?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  biometricsEnabled?: boolean;
  pin?: string;
}

// Session interface
export interface Session {
  isApproved: boolean;
  isVerified?: boolean;
  user?: User;
}

// AuthContext interface - implement this in your project
export interface AuthContextType {
  status: 'loading' | 'authenticated' | 'unauthenticated';
  session: Session | null;
  user: User | null;
  isLoading: boolean;
  
  // Methods your AuthContext should implement
  login: (email: string, password: string) => Promise<void>;
  registerUser: (email: string, password: string, userData: any) => Promise<void>;
  uploadCredentials: (credentials: Record<string, any>) => Promise<void>;
  signOut: () => Promise<void>;
  
  // Optional methods for forgot password
  sendForgotPasswordEmail?: (email: string) => Promise<void>;
  resetPassword?: (email: string, newPassword: string, confirmPassword: string) => Promise<void>;
}

// Theme context interface
export interface ThemeContextType {
  isDark: boolean;
  toggleTheme: () => void;
}

// Export hook interfaces for easy implementation
export const useAuth = (): AuthContextType => {
  throw new Error('useAuth must be implemented in your project');
};

export const useTheme = (): ThemeContextType => {
  throw new Error('useTheme must be implemented in your project');
}; 