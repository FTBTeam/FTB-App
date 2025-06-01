import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed } from 'vue';
import { constants } from '@/core/constants.ts';
import {useAccountsStore} from "@/store/accountsStore.ts";
import {Roles} from "@/core/auth/jwtIdTokenData";

function idTokenHasPatreonAccess(resourceAccess: Record<string, Roles> | undefined): boolean {
  if (!resourceAccess) return false;
  
  const values = Object.values(resourceAccess).flatMap(e => e.roles);
  return values.includes("patreon:ads");
}

export function useAds() {
  const isProd = constants.isProduction;
  const appSettingsStore = useAppSettings();
  const accountStore = useAccountsStore();
  
  const adsEnabled = computed(() => {
    const isPatreon = idTokenHasPatreonAccess(accountStore.ftbAccount?.idTokenData?.resource_access);
    if (isPatreon) {
      return false;
    }
    
    // Not patreon? Okay, are we in dev and debug ads disabled?
    return !(!isProd && appSettingsStore.debugAdsDisabled);
  });
  
  return {
    adsEnabled
  }
}