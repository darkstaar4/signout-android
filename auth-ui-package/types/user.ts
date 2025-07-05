export type UserRole = 'superadmin' | 'admin' | 'user';

export type User = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  npiNumber: string;
  professionalTitle: string;
  specialty: string;
  officeAddress: string;
  officeCity: string;
  officeState: string;
  officeZip: string;
  officeCountry: string;
  country: string;
  role: UserRole;
  isApproved: boolean;
  isDisabled: boolean;
  isDeleted: boolean;
  createdAt: Date;
  updatedAt: Date;
  disabledReason?: string;
  hospitals?: Hospital[];
  biometricsEnabled: boolean;
  pin: string;
  matrixUsername: string;
  matrixPassword: string;
};

export type Hospital = {
  id: string;
  name: string;
  address: string;
  department?: string;
  privileges?: string;
};

export type RegistrationRequest = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  npiNumber?: string;
  specialty: string;
  submittedAt: Date;
  documents: {
    idDocument?: DocumentInfo;
    npi?: DocumentInfo;
    dea?: DocumentInfo;
    state?: DocumentInfo;
  };
  status: 'pending' | 'approved' | 'rejected';
  rejectionReason?: string;
  reviewedBy?: string;
  reviewedAt?: Date;
};

export type DocumentInfo = {
  uri?: string;
  name: string;
  type: string;
  size?: number;
  hasDocument?: boolean;
};

export type BroadcastMessage = {
  id: string;
  title: string;
  content: string;
  type: 'announcement' | 'maintenance' | 'emergency' | 'policy';
  audience: 'all' | 'admins' | 'users-only';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  displayType: 'banner' | 'popup';
  isActive: boolean;
  createdBy: string;
  createdAt: Date;
  expiresAt?: Date;
  dismissible: boolean;
  // New scheduling fields
  isScheduled: boolean;
  scheduledFor?: Date;
  timezone?: string;
};