import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed } from 'vue';
import { constants } from '@/core/constants.ts';

// TODO: Add back settings.apperance.showAds when we support logging in again
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