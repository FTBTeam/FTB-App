import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed } from 'vue';
import { constants } from '@/core/constants.ts';
import {useAccountsStore} from "@/store/accountsStore.ts";
import {Roles} from "@/core/auth/jwtIdTokenData";

function idTokenHasPremiumAccess(resourceAccess: Record<string, Roles> | undefined): boolean {
  if (!resourceAccess) return false;
  
  return resourceAccess?.["ftb-app"]?.roles.includes("member:ads");
}

export function useAds() {
  const isProd = constants.isProduction;
  const appSettingsStore = useAppSettings();
  const accountStore = useAccountsStore();
  
  const adsEnabled = computed(() => {
    const isPremium = idTokenHasPremiumAccess(accountStore.ftbAccount?.idTokenData?.resource_access);
    if (isPremium) {
      return false;
    }
    
    // Not premium? Okay, are we in dev and debug ads disabled?
    return !(!isProd && appSettingsStore.debugAdsDisabled);
  });
  
  return {
    adsEnabled
  }
}