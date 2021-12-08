import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';

const validateToken = async (profile?: AuthProfile) => {
  console.log(profile);
  if (profile != null) {
    if (profile.type === 'microsoft') {
      // TODO: validate microsoft token
      // We need to store when the token expires using expires_in
      return false;
    } else if (profile.type === 'mojang') {
      let rawResponse = await fetch(`https://authserver.mojang.com/validate`, {
        headers: {
          'Content-Type': 'application/json',
        },
        method: 'POST',
        body: JSON.stringify({
          accessToken: profile.tokens.accessToken,
          clientToken: (profile.tokens as any).clientToken,
        }),
      });
      if (rawResponse.status === 204) {
        return false;
      } else {
        return true;
      }
    }
  } else {
    // TODO: There's no active profile
  }
};

const refreshToken = async (profile?: AuthProfile) => {
  if (profile != null) {
    if (profile.type === 'microsoft') {
      // TODO: refresh microsoft token
    } else if (profile.type === 'mojang') {
      let rawResponse = await fetch(`https://authserver.mojang.com/refresh`, {
        headers: {
          'Content-Type': 'application/json',
        },
        method: 'POST',
        body: JSON.stringify({
          accessToken: profile.tokens.accessToken,
          clientToken: (profile.tokens as any).clientToken,
        }),
      });
      let response = await rawResponse.json();
      console.log(response.accessToken);
      // TODO: Handle when this doesn't work and ask for password again
      // TODO: update the tokens stored on the profile
    }
  } else {
    // TODO: There's no active profile
  }
};

// 🚀
export const preLaunchChecks = async () => {
  const { 'core/getProfiles': profiles, 'core/getActiveProfile': activeProfile } = store.getters;

  if (profiles.length === 0) {
    await store.dispatch('core/openSignIn', null, { root: true });
    return;
  }

  let profile = profiles.find((profile: AuthProfile) => profile.uuid == activeProfile.uuid);

  const shouldRefresh = await validateToken(profile);
  if (shouldRefresh) {
    console.log('We need to refresh');
    await refreshToken(profile);
    return false;
  }

  return false;
};
