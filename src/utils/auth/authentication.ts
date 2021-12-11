import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';

/**
 * Attempt to valid the user's token
 * @param profile Auth Profile
 */
const isRefreshRequired = async (profile?: AuthProfile) => {
  if (!profile) {
    return false;
  }

  if (profile.type === 'microsoft') {
    // TODO: validate microsoft token
    // We need to store when the token expires using expires_in
    return true;
  } else if (profile.type === 'mojang') {
    try {
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

      return rawResponse.status !== 204;
    } catch {
      return true;
    }
  }
};

const refreshToken = async (profile?: AuthProfile) => {
  if (profile != null) {
    console.log(profile);
    if (profile.type === 'microsoft') {
      // TODO: refresh microsoft token
      let rawResponse = await fetch(`https://msauth.feed-the-beast.com/v1/authenticate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: (profile.tokens as any).refreshToken,
          refresh: true,
        }),
      });

      let response = await rawResponse.json();
      if (response.iv) {
        let res = await fetch(`https://msauth.feed-the-beast.com/v1/retrieve`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(response),
        });

        console.log(await res.json());
      }
    } else if (profile.type === 'mojang') {
      try {
        let rawResponse = await fetch(`https://authserver.mojang.com/refresh`, {
          headers: {
            'Content-Type': 'application/json',
          },
          method: 'POST',
          body: JSON.stringify({
            accessToken: profile.tokens.accessToken,
            clientToken: (profile.tokens as any).clientToken,
            requestUser: true,
          }),
        });
        let response = await rawResponse.json();
        console.log(response.accessToken);
        // TODO: Handle when this doesn't work and ask for password again
        // TODO: update the tokens stored on the profile
      } catch (e) {
        console.log('hello');
      }
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

  const shouldRefresh = await isRefreshRequired(profile);
  if (shouldRefresh) {
    console.log('We need to refresh');
    await refreshToken(profile);
    return false;
  }

  return false;
};
