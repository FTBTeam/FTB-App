import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';
import { addHyphensToUuid } from '../helpers';

/**
 * Attempt to valid the user's token
 * @param profile Auth Profile
 */
const isRefreshRequired = async (profile?: AuthProfile) => {
  if (!profile) {
    return false;
  }

  if (profile.type === 'microsoft') {
    if (profile.expiresAt) {
      return profile.expiresAt > new Date().valueOf();
    }
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

        const data = await res.json();
        const id: string = data.data.minecraftUuid;
        const newUuid = addHyphensToUuid(id);

        store.dispatch('sendMessage',{
          payload: {
            type: 'profiles.updateMs',
            uuid: profile.uuid,
            ...data.data,
            minecraftUuid: id.includes('-') ? id : newUuid,
          },
          callback: (e: any) => {
            if (e.success) {
              store.dispatch('core/loadProfiles');
            } else {
              // TODO: Failed to update the profile
            }
          },
        });
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
        if (rawResponse.status === 403) {
          // TODO: Show pop up box to get password
          await store.dispatch('core/openSignIn', {open: true, jumpToAuth: 'mc', uuid: profile.uuid}, { root: true });
          return false;
        }
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
    return true;
  }

  let profile = profiles.find((profile: AuthProfile) => profile.uuid == activeProfile.uuid);

  const shouldRefresh = await isRefreshRequired(profile);
  if (shouldRefresh) {
    console.log('We need to refresh');
    if (await refreshToken(profile)) {
      return false;
    } else {
      return true;
    }
  }

  return false;
};
