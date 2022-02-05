import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';
import { addHyphensToUuid, wsTimeoutWrapper } from '../helpers';
import dayjs from 'dayjs';

interface Authenticator {
  refresh: (profile: AuthProfile) => Promise<boolean>;
  valid: (profile: AuthProfile) => Promise<boolean>;
}

const msAuthenticator: Authenticator = {
  async valid(profile: AuthProfile): Promise<boolean> {
    return profile.expiresAt ? Math.round(Date.now() / 1000) < profile.expiresAt : false;
  },

  async refresh(profile: AuthProfile): Promise<boolean> {
    try {
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

        const wsRes = await wsTimeoutWrapper({
          type: 'profiles.updateMs',
          uuid: profile.uuid,
          ...data.data,
          minecraftUuid: id.includes('-') ? id : newUuid,
        });

        if (wsRes.success) {
          await store.dispatch('core/loadProfiles');
          return true;
        } else {
          console.log('Something went fatally wrong with updating on the Websockets for profiles.updateMs');
          return false;
        }
      } else {
        console.log('Unable to refresh token due to missing encryption details', response);
        return false;
      }
    } catch (e) {
      console.log(e);
      return false;
    }
  },
};

const mcAuthenticator: Authenticator = {
  async valid(profile: AuthProfile): Promise<boolean> {
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

      return rawResponse.status === 204;
    } catch {
      return false;
    }
  },

  async refresh(profile: AuthProfile): Promise<boolean> {
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
        await store.dispatch('core/openSignIn', { open: true, jumpToAuth: 'mc', uuid: profile.uuid }, { root: true });
        return false;
      }
      let response = await rawResponse.json();
      if (!response.accessToken) {
        await store.dispatch('core/openSignIn', { open: true, jumpToAuth: 'mc', uuid: profile.uuid }, { root: true });
        return false;
      }

      return true;
    } catch (e) {
      console.error(e);
      return false;
    }
  },
};

const purgeMinecraftProfiles = async (profiles: AuthProfile[]) => {
  const minecraftProfiles = profiles.filter((profile) => profile.type !== 'microsoft');

  for (let minecraftProfile of minecraftProfiles) {
    try {
      await wsTimeoutWrapper({
        type: 'profiles.remove',
        uuid: minecraftProfile.uuid,
      });
    } catch {
      console.log('Failed to remove profile');
    }
  }

  await store.dispatch('core/loadProfiles');

  return minecraftProfiles;
};

// 🚀
export const preLaunchChecksValid = async () => {
  const { 'core/getProfiles': profiles, 'core/getActiveProfile': activeProfile } = store.getters;

  if (profiles.length === 0) {
    await store.dispatch('core/openSignIn', { open: true }, { root: true });
    return false;
  }

  const profile: AuthProfile | null = profiles.find((profile: AuthProfile) => profile.uuid == activeProfile.uuid);
  if (!profile) {
    await store.dispatch('core/openSignIn', { open: true }, { root: true });
    return false;
  }

  const validator = profile.type === 'microsoft' ? msAuthenticator : mcAuthenticator;
  if (dayjs().isAfter('2022-01-10')) {
    // Use this chance to purge all old profiles
    const removedProfiles = await purgeMinecraftProfiles(profiles);
    const removedOurProfile = removedProfiles.find((p: AuthProfile) => p.uuid === profile.uuid);

    for (let removedProfile of removedProfiles) {
      console.log('Notifying');
      await store.dispatch('showAlert', {
        type: 'warning',
        title: 'Profile removed',
        message: `We've automatically removed ${removedProfile.username} as it was added using the old authentication system.`,
      });
    }

    if (removedOurProfile) {
      await store.dispatch('core/openSignIn', { open: true }, { root: true });
    }

    return false;
  }

  const isValid = await validator.valid(profile);
  if (!isValid) {
    return await validator.refresh(profile);
  }

  return true;
};
