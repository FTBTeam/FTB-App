import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed } from 'vue';
import { constants } from '@/core/constants.ts';
import {useAccountsStore} from "@/store/accountsStore.ts";

export function useAds() {
  const isProd = constants.isProduction;
  const appSettingsStore = useAppSettings();
  const accountStore = useAccountsStore();
  
  const adsEnabled = computed(() => {
    if (accountStore.isPremiumMember) {
      return false;
    }
    
    // Not premium? Okay, are we in dev and debug ads disabled?
    return !(!isProd && appSettingsStore.debugAdsDisabled);
  });
  
  return {
    adsEnabled
  }
}