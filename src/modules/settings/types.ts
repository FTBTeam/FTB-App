import {SettingsData} from '@/core/@types/javaApi';

export interface Hardware {
  totalMemory: number;
  totalCores: number;
  availableMemory: number;
  mainScreen: Resolution;
  supportedResolutions: Resolution[];
}

export interface SettingsState {
  settings: SettingsData;
  error: boolean;
  hardware: Hardware;
}

export interface JavaVersion {
  name: string;
  path: string;
}

export interface Resolution {
  width: number;
  height: number;
}
