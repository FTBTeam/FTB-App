import {sendMessage} from '@/core/websockets/websocketsApi';
import store from '@/modules/store';

export type LimitedCheckResult = "VALID" | "NOT_LOGGED_IN" | "TOTAL_FAILURE"

export async function safeCheckProfileActive(profileUuid: string): Promise<LimitedCheckResult> {
  try {
    return await checkProfileActive(profileUuid);
  } catch (error) {
    console.error(error);
  }
  
  return "TOTAL_FAILURE"
}

async function checkProfileActive(profileUuid: string): Promise<LimitedCheckResult> {
  const res = await sendMessage("profiles.is-valid", {
    profileUuid: profileUuid
  })
  
  if (res.checkResult === "EXPIRED") {
    const refreshResult = await refreshProfile(profileUuid);
    if (!refreshResult) {
      return "TOTAL_FAILURE";
    }
    
    if (refreshResult) {
      return "VALID";
    }
  }
  
  // Expired is handled so now it's a limited check
  return res.checkResult as LimitedCheckResult;
}

async function refreshProfile(profileUuid: string): Promise<boolean> {
  const res = await sendMessage("profiles.refresh", {
    profileUuid: profileUuid
  })
  
  if (res.success) {
    // Get the app to reload the profiles
    await store.dispatch('core/loadProfiles');
    return true;
  }
  
  return false;
}