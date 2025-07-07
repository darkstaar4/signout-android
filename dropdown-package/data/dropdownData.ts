// Dropdown data for registration forms
// You can customize these lists based on your needs

export interface DropdownOption {
  label: string;
  value: string;
}

export const PROFESSIONAL_TITLES: DropdownOption[] = [
  { label: 'MD', value: 'MD' },
  { label: 'DO', value: 'DO' },
  { label: 'PA-C', value: 'PA-C' },
  { label: 'NP', value: 'NP' },
  { label: 'RN', value: 'RN' },
  { label: 'LPN', value: 'LPN' },
  { label: 'CNA', value: 'CNA' },
  { label: 'PharmD', value: 'PharmD' },
  { label: 'DDS', value: 'DDS' },
  { label: 'DMD', value: 'DMD' },
  { label: 'DPT', value: 'DPT' },
  { label: 'OTR', value: 'OTR' },
  { label: 'SLP', value: 'SLP' },
  { label: 'Student', value: 'Student' },
  { label: 'Other', value: 'Other' },
];

export const COUNTRIES: DropdownOption[] = [
  { label: '🇺🇸 United States', value: 'USA' },
  { label: '🇨🇦 Canada', value: 'Canada' },
  { label: '🇲🇽 Mexico', value: 'Mexico' },
  { label: '🇬🇧 United Kingdom', value: 'UK' },
  { label: '🇦🇺 Australia', value: 'Australia' },
  { label: '🇩🇪 Germany', value: 'Germany' },
  { label: '🇫🇷 France', value: 'France' },
  { label: '🇮🇹 Italy', value: 'Italy' },
  { label: '🇪🇸 Spain', value: 'Spain' },
  { label: '🇳🇱 Netherlands', value: 'Netherlands' },
  { label: '🇧🇷 Brazil', value: 'Brazil' },
  { label: '🇦🇷 Argentina', value: 'Argentina' },
  { label: '🇯🇵 Japan', value: 'Japan' },
  { label: '🇰🇷 South Korea', value: 'South Korea' },
  { label: '🇮🇳 India', value: 'India' },
  { label: '🇨🇳 China', value: 'China' },
];

export const US_STATES: DropdownOption[] = [
  { label: 'Alabama', value: 'AL' },
  { label: 'Alaska', value: 'AK' },
  { label: 'Arizona', value: 'AZ' },
  { label: 'Arkansas', value: 'AR' },
  { label: 'California', value: 'CA' },
  { label: 'Colorado', value: 'CO' },
  { label: 'Connecticut', value: 'CT' },
  { label: 'Delaware', value: 'DE' },
  { label: 'Florida', value: 'FL' },
  { label: 'Georgia', value: 'GA' },
  { label: 'Hawaii', value: 'HI' },
  { label: 'Idaho', value: 'ID' },
  { label: 'Illinois', value: 'IL' },
  { label: 'Indiana', value: 'IN' },
  { label: 'Iowa', value: 'IA' },
  { label: 'Kansas', value: 'KS' },
  { label: 'Kentucky', value: 'KY' },
  { label: 'Louisiana', value: 'LA' },
  { label: 'Maine', value: 'ME' },
  { label: 'Maryland', value: 'MD' },
  { label: 'Massachusetts', value: 'MA' },
  { label: 'Michigan', value: 'MI' },
  { label: 'Minnesota', value: 'MN' },
  { label: 'Mississippi', value: 'MS' },
  { label: 'Missouri', value: 'MO' },
  { label: 'Montana', value: 'MT' },
  { label: 'Nebraska', value: 'NE' },
  { label: 'Nevada', value: 'NV' },
  { label: 'New Hampshire', value: 'NH' },
  { label: 'New Jersey', value: 'NJ' },
  { label: 'New Mexico', value: 'NM' },
  { label: 'New York', value: 'NY' },
  { label: 'North Carolina', value: 'NC' },
  { label: 'North Dakota', value: 'ND' },
  { label: 'Ohio', value: 'OH' },
  { label: 'Oklahoma', value: 'OK' },
  { label: 'Oregon', value: 'OR' },
  { label: 'Pennsylvania', value: 'PA' },
  { label: 'Rhode Island', value: 'RI' },
  { label: 'South Carolina', value: 'SC' },
  { label: 'South Dakota', value: 'SD' },
  { label: 'Tennessee', value: 'TN' },
  { label: 'Texas', value: 'TX' },
  { label: 'Utah', value: 'UT' },
  { label: 'Vermont', value: 'VT' },
  { label: 'Virginia', value: 'VA' },
  { label: 'Washington', value: 'WA' },
  { label: 'West Virginia', value: 'WV' },
  { label: 'Wisconsin', value: 'WI' },
  { label: 'Wyoming', value: 'WY' },
];

export const CANADIAN_PROVINCES: DropdownOption[] = [
  { label: 'Alberta', value: 'AB' },
  { label: 'British Columbia', value: 'BC' },
  { label: 'Manitoba', value: 'MB' },
  { label: 'New Brunswick', value: 'NB' },
  { label: 'Newfoundland and Labrador', value: 'NL' },
  { label: 'Northwest Territories', value: 'NT' },
  { label: 'Nova Scotia', value: 'NS' },
  { label: 'Nunavut', value: 'NU' },
  { label: 'Ontario', value: 'ON' },
  { label: 'Prince Edward Island', value: 'PE' },
  { label: 'Quebec', value: 'QC' },
  { label: 'Saskatchewan', value: 'SK' },
  { label: 'Yukon', value: 'YT' },
];

export const MEDICAL_SPECIALTIES: DropdownOption[] = [
  { label: 'Student', value: 'Student' },
  { label: 'Other', value: 'Other' },
  { label: 'Addiction Medicine', value: 'Addiction Medicine' },
  { label: 'Allergy and Immunology', value: 'Allergy and Immunology' },
  { label: 'Anesthesiology', value: 'Anesthesiology' },
  { label: 'Cardiology', value: 'Cardiology' },
  { label: 'Cardiothoracic Surgery', value: 'Cardiothoracic Surgery' },
  { label: 'Critical Care Medicine', value: 'Critical Care Medicine' },
  { label: 'Dermatology', value: 'Dermatology' },
  { label: 'Emergency Medicine', value: 'Emergency Medicine' },
  { label: 'Endocrinology', value: 'Endocrinology' },
  { label: 'Family Medicine', value: 'Family Medicine' },
  { label: 'Gastroenterology', value: 'Gastroenterology' },
  { label: 'General Surgery', value: 'General Surgery' },
  { label: 'Geriatric Medicine', value: 'Geriatric Medicine' },
  { label: 'Hematology', value: 'Hematology' },
  { label: 'Hematology/Oncology', value: 'Heme/Onc' },
  { label: 'Hospitalist', value: 'Hospitalist' },
  { label: 'Infectious Disease', value: 'Infectious Disease' },
  { label: 'Internal Medicine', value: 'Internal Medicine' },
  { label: 'Interventional Cardiology', value: 'Interventional Cardiology' },
  { label: 'Interventional Radiology', value: 'Interventional Radiology' },
  { label: 'Nephrology', value: 'Nephrology' },
  { label: 'Neurology', value: 'Neurology' },
  { label: 'Neurosurgery', value: 'Neurosurgery' },
  { label: 'Nuclear Medicine', value: 'Nuclear Medicine' },
  { label: 'Obesity Medicine', value: 'Obesity Medicine' },
  { label: 'Obstetrics & Gynecology', value: 'Obstetrics & Gynecology' },
  { label: 'Occupational Medicine', value: 'Occupational Medicine' },
  { label: 'Oncology', value: 'Oncology' },
  { label: 'Ophthalmology', value: 'Ophthalmology' },
  { label: 'Orthopedic Surgery', value: 'Orthopedic Surgery' },
  { label: 'Otolaryngology', value: 'Otolaryngology' },
  { label: 'Pain Medicine', value: 'Pain Medicine' },
  { label: 'Palliative Care', value: 'Palliative Care' },
  { label: 'Pathology', value: 'Pathology' },
  { label: 'Pediatrics', value: 'Pediatrics' },
  { label: 'Physical Medicine & Rehabilitation', value: 'Physical Medicine & Rehabilitation' },
  { label: 'Plastic Surgery', value: 'Plastic Surgery' },
  { label: 'Podiatry', value: 'Podiatry' },
  { label: 'Preventive Medicine', value: 'Preventive Medicine' },
  { label: 'Psychiatry', value: 'Psychiatry' },
  { label: 'Psychology', value: 'Psychology' },
  { label: 'Pulmonology', value: 'Pulmonology' },
  { label: 'Radiation Oncology', value: 'Radiation Oncology' },
  { label: 'Radiology', value: 'Radiology' },
  { label: 'Registered Nurse', value: 'Registered Nurse' },
  { label: 'Rheumatology', value: 'Rheumatology' },
  { label: 'Sleep Medicine', value: 'Sleep Medicine' },
  { label: 'Sports Medicine', value: 'Sports Medicine' },
  { label: 'Trauma Surgery', value: 'Trauma Surgery' },
  { label: 'Urgent Care', value: 'Urgent Care' },
  { label: 'Urology', value: 'Urology' },
  { label: 'Vascular Surgery', value: 'Vascular Surgery' },
];

// Country codes for phone number input
export interface CountryCode {
  code: string;
  name: string;
  dialCode: string;
  flag: string;
}

export const COUNTRY_CODES: CountryCode[] = [
  { code: 'US', name: 'United States', dialCode: '+1', flag: '🇺🇸' },
  { code: 'CA', name: 'Canada', dialCode: '+1', flag: '🇨🇦' },
  { code: 'MX', name: 'Mexico', dialCode: '+52', flag: '🇲🇽' },
  { code: 'GB', name: 'United Kingdom', dialCode: '+44', flag: '🇬🇧' },
  { code: 'AU', name: 'Australia', dialCode: '+61', flag: '🇦🇺' },
  { code: 'DE', name: 'Germany', dialCode: '+49', flag: '🇩🇪' },
  { code: 'FR', name: 'France', dialCode: '+33', flag: '🇫🇷' },
  { code: 'IT', name: 'Italy', dialCode: '+39', flag: '🇮🇹' },
  { code: 'ES', name: 'Spain', dialCode: '+34', flag: '🇪🇸' },
  { code: 'NL', name: 'Netherlands', dialCode: '+31', flag: '🇳🇱' },
  { code: 'BR', name: 'Brazil', dialCode: '+55', flag: '🇧🇷' },
  { code: 'AR', name: 'Argentina', dialCode: '+54', flag: '🇦🇷' },
  { code: 'JP', name: 'Japan', dialCode: '+81', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', dialCode: '+82', flag: '🇰🇷' },
  { code: 'IN', name: 'India', dialCode: '+91', flag: '🇮🇳' },
  { code: 'CN', name: 'China', dialCode: '+86', flag: '🇨🇳' },
];

// Helper function to get states/provinces based on country
export const getStatesForCountry = (countryValue: string): DropdownOption[] => {
  switch (countryValue) {
    case 'USA':
      return US_STATES;
    case 'Canada':
      return CANADIAN_PROVINCES;
    default:
      return [];
  }
};

// Helper function to check if a country requires state selection
export const countryRequiresState = (countryValue: string): boolean => {
  return ['USA', 'Canada'].includes(countryValue);
}; 