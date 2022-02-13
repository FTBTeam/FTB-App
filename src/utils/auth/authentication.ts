import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';
import { addHyphensToUuid, wsTimeoutWrapper } from '../helpers';
import dayjs from 'dayjs';
import { finishAuthentication } from '@/utils/auth/msAuthentication';
import { issueCodes } from '@/components/templates/authentication/MicrosoftAuth.vue';

interface Authenticator {
  refresh: (profile: AuthProfile) => Promise<boolean>;
  valid: (profile: AuthProfile) => Promise<boolean>;
}

// Todo: reduce logging in the future

export const logAuth = (level: 'debug' | 'warn' | 'error', message: any) =>
  console.log(`[${dayjs().format('DD/MM/YY hh:mm:ss')}] [${level}] [auth] ${message}`);

const msAuthenticator: Authenticator = {
  async valid(profile: AuthProfile): Promise<boolean> {
    const profileIsValid = profile.expiresAt ? Math.round(Date.now() / 1000) < profile.expiresAt : false;
    logAuth('debug', `Profile is ${profileIsValid ? 'valid' : 'invalid'}`);
    return !profileIsValid;
  },

  async refresh(profile: AuthProfile): Promise<boolean> {
    logAuth('debug', `Asking the refresh service to refresh the token for ${profile.username}`);

    try {
      let rawResponse = await fetch(`https://msauth.feed-the-beast.com/v1/authenticate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: (profile.tokens as any).refreshToken,
          refresh: true,
          useNewFlow: true,
        }),
      });

      logAuth('debug', `Sending the refresh request to the refresh service for ${profile.username}`);

      let response = await rawResponse.json();
      if (response.iv) {
        logAuth('debug', `Received a valid set of encryption details to decrypt authentication data`);

        let res = await fetch(`https://msauth.feed-the-beast.com/v1/retrieve`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(response),
        });

        logAuth('debug', `Sending retrieve request to decrypt the authentication data`);
        const data = await res.json();

        // Use the new flow
        const authRes = await finishAuthentication(
          data.data.liveAccessToken,
          data.data.liveRefreshToken,
          data.data.liveExpires,
        );

        if (!authRes || authRes.code) {
          logAuth(
            'error',
            `Failed to authenticate with the refresh service for ${profile.username} because ${
              authRes.code ? issueCodes[authRes.code] : 'failed...'
            }`,
          );
          return false;
        }

        const id: string = authRes.minecraftUuid ?? '';
        const newUuid = addHyphensToUuid(id);

        const wsRes = await wsTimeoutWrapper({
          type: 'profiles.updateMs',
          uuid: profile.uuid,
          ...authRes,
          minecraftUuid: id.includes('-') ? id : newUuid,
        });

        if (wsRes.success) {
          logAuth('debug', `We've successfully logged in via a refresh action. Reloading profiles...`);
          await store.dispatch('core/loadProfiles');
          return true;
        } else {
          logAuth('error', `Fatal error, unable to retrieve profiles / update profiles`);
          console.log('Something went fatally wrong with updating on the Websockets for profiles.updateMs');
          return false;
        }
      } else {
        logAuth('error', `No encryption details, we can not proceed...`);
        console.log('Unable to refresh token due to missing encryption details', response);
        return false;
      }
    } catch (e) {
      logAuth('error', `Request errored with the response of ${(e as any).message}`);
      console.log(e);
      return false;
    }
  },
};

const mcAuthenticator: Authenticator = {
  async valid(profile: AuthProfile): Promise<boolean> {
    try {
      logAuth('debug', `Sending request to ask the Mojang servers if the token is valid`);
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

      logAuth('debug', `Received a response from the Mojang servers with the status of ${rawResponse.status}`);
      logAuth('debug', `The profile is ${rawResponse.status === 204 ? 'valid' : 'invalid'}`);
      return rawResponse.status === 204;
    } catch {
      logAuth('warn', `Validation request errored, falling back to assume the authentication is invalid`);
      return false;
    }
  },

  async refresh(profile: AuthProfile): Promise<boolean> {
    try {
      logAuth('debug', `Asking the mojang servers to refresh the token for ${profile.username}`);
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
        logAuth('error', `No profile, signin again`);
        await store.dispatch('core/openSignIn', { open: true, jumpToAuth: 'mc', uuid: profile.uuid }, { root: true });
        return false;
      }

      let response = await rawResponse.json();
      if (!response.accessToken) {
        logAuth('error', `No access token, we can not proceed...`);
        await store.dispatch('core/openSignIn', { open: true, jumpToAuth: 'mc', uuid: profile.uuid }, { root: true });
        return false;
      }

      logAuth('debug', `Profile refresh request succeeded, updating the profile...`);
      return true;
    } catch (e) {
      logAuth('error', `Request errored with the response of ${(e as any).message}`);
      console.error(e);
      return false;
    }
  },
};

const purgeMinecraftProfiles = async (profiles: AuthProfile[]) => {
  const minecraftProfiles = profiles.filter((profile) => profile.type !== 'microsoft');

  logAuth('debug', `Pruning ${minecraftProfiles.length} Minecraft profiles`);

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

  logAuth('debug', `Loading profiles again`);
  await store.dispatch('core/loadProfiles');

  return minecraftProfiles;
};

// 🚀
export const preLaunchChecksValid = async (tryAgainInstanceUuid: any) => {
  const { 'core/getProfiles': profiles, 'core/getActiveProfile': activeProfile } = store.getters;

  const signInAgain = async () => {
    await store.dispatch('core/openSignIn', { open: true, tryAgainInstanceUuid }, { root: true });
  };

  if (profiles.length === 0) {
    logAuth('debug', 'No profiles found, prompting user to sign in');
    await signInAgain();
    return false;
  }

  let profile: AuthProfile | null = profiles.find((profile: AuthProfile) => profile.uuid == activeProfile.uuid);
  if (!profile) {
    logAuth('debug', 'No active profile found, prompting user to sign in');
    await signInAgain();
    return false;
  }

  // TODO: remove in April 2022
  if (dayjs().isAfter('2022-03-10')) {
    logAuth('debug', 'triggered catch all for accounts past the 10/03/2022');
    // Use this chance to purge all old profiles
    const removedProfiles = await purgeMinecraftProfiles(profiles);
    const removedOurProfile = removedProfiles.find((p: AuthProfile) => p.uuid === profile?.uuid);

    for (let removedProfile of removedProfiles) {
      logAuth(
        'debug',
        `Found profile ${removedProfile.username} with uuid ${removedProfile.uuid} which is using Mojang auth. Removing...`,
      );
      await store.dispatch('showAlert', {
        type: 'warning',
        title: 'Profile removed',
        message: `We've automatically removed ${removedProfile.username} as it was added using the old authentication system.`,
      });
    }

    if (removedOurProfile) {
      logAuth('debug', `The active profile was removed, attempting to fall back`);
      // Show remaining profiles
      const remainingProfile = profiles.length > 0 ? profiles[0] : null;
      if (remainingProfile) {
        logAuth(
          'debug',
          `Successfully found a fallback profile ${remainingProfile.username} with uuid ${remainingProfile.uuid}`,
        );
        profile = remainingProfile;

        // Alert the user
        await store.dispatch('showAlert', {
          type: 'primary',
          title: 'Using Microsoft account instead',
          message: `We've automatically switched your active account to ${profile?.username}`,
        });

        // Set the active profile to the remaining profile
        try {
          const data = await wsTimeoutWrapper({
            type: 'profiles.setActiveProfile',
            uuid: remainingProfile.uuid,
          });

          if (data.success) {
            await store.dispatch('core/loadProfiles');
            logAuth(
              'debug',
              `Successfully set active profile to ${remainingProfile.username} with uuid ${remainingProfile.uuid}`,
            );
          } else {
            logAuth('error', `Failed to set active profile`);
          }
        } catch {
          logAuth('error', `Failed to set active profile`);
          console.log('Failed to set active profile');
        }
      } else {
        logAuth('debug', `No remaining profiles found, prompting user to sign in`);
        await signInAgain();
        return false;
      }
    }
  }

  // TODO: end of remove in April 2022

  // Something went really wrong here...
  if (profile == null) {
    logAuth('error', `Fatal error, falling back to sign in`);
    await signInAgain();
    return false;
  }

  const validator = profile.type === 'microsoft' ? msAuthenticator : mcAuthenticator;
  logAuth(
    'debug',
    `Validating profile ${profile.username} with uuid ${profile.uuid} against ${
      profile.type === 'microsoft' ? 'Microsoft' : 'Mojang'
    }`,
  );

  const isValid = await validator.valid(profile);
  logAuth('debug', `The users token is ${isValid ? 'valid' : 'invalid'}`);
  if (!isValid) {
    logAuth('debug', `Found a profile that no longer can be validated. Trying to refresh`);
    const refresh = await validator.refresh(profile);
    if (!refresh) {
      logAuth('error', `Failed to refresh token`);
    }

    logAuth('debug', `The refresh was ${refresh ? 'successful' : 'unsuccessful'}`);
    return refresh;
  }

  logAuth('debug', `Successfully authenticated ${profile.username} with uuid ${profile.uuid}`);
  return true;
};
