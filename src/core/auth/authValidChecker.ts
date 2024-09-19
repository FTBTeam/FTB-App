import {sendMessage} from '@/core/websockets/websocketsApi';

export async function safeCheckProfileActive(profileUuid: string): Promise<boolean> {
  try {
    return await checkProfileActive(profileUuid);
  } catch (error) {
    console.error(error);
  }
  
  return false;
}

async function checkProfileActive(profileUuid: string): Promise<boolean> {
  const res = await sendMessage("profiles.is-valid", {
    profileUuid: profileUuid
  })
  
  if (!res.success) {
    const refreshResult = await refreshProfile(profileUuid);
    if (!refreshResult) {
      return false;
    }
  }
  
  return true;
}

async function refreshProfile(profileUuid: string): Promise<boolean> {
  const res = await sendMessage("profiles.refresh", {
    profileUuid: profileUuid
  })
  
  return res.success;
}