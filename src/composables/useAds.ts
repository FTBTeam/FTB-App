import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed } from 'vue';
import { constants } from '@/core/constants.ts';

export function useAds() {
  const isProd = constants.isProduction;
  const appSettingsStore = useAppSettings();
  
  const adsEnabled = computed(() => {
    if (isProd) return true;
    
    return appSettingsStore.debugAdsDisabled;
  });
  
  return {
    adsEnabled
  }
}