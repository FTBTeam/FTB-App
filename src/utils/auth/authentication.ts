import { AuthProfile } from '@/modules/core/core.types';
import store from '@/modules/store';
import { wsTimeoutWrapper } from '@/utils';
import dayjs from 'dayjs';
import { createError } from '@/core/errors/errorCodes';

interface Authenticator {
  refresh: (profile: AuthProfile) => Promise<boolean>;
}

// Todo: reduce logging in the future

export const logAuth = (level: 'debug' | 'warn' | 'error', message: any) =>
  console.log(`[${dayjs().format('DD/MM/YY hh:mm:ss')}] [${level}] [auth] ${message}`);

const msAuthenticator: Authenticator = {
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
        const data = (await res.json()).data;

        const authenticator = await wsTimeoutWrapper({
          type: 'profiles.refresh',
          profileUuid: profile.uuid,
          ...data,
        });

        if (authenticator?.response === 'updated') {
          logAuth('debug', `Successfully refreshed the token for ${profile.username}`);
          return true;
        } else {
          logAuth(
            'warn',
            `Failed to refresh the token for ${profile.username} due to ${authenticator?.response ?? 'unknown'}`,
          );
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
  async refresh(profile: AuthProfile): Promise<boolean> {
    const authenticator = await wsTimeoutWrapper({
      type: 'profiles.refresh',
      profileUuid: profile.uuid,
    });

    if (authenticator?.response === 'updated') {
      logAuth('debug', `Successfully refreshed the token for ${profile.username}`);
      return true;
    } else {
      logAuth(
        'warn',
        `Failed to refresh the token for ${profile.username} due to ${authenticator?.response ?? 'unknown'}`,
      );
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

const attemptToRemoveMojangAccounts = async (
  profiles: AuthProfile[],
  profile: AuthProfile,
  signInAgain: () => void,
) => {
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
};

export type LaunchCheckResult = {
  ok: boolean;
  requiresSignIn: boolean;
  quite?: boolean;
  error?: {
    message: string;
    code: string;
  };
};

export const removeActiveProfile = async (): Promise<boolean> => {
  const { 'core/getActiveProfile': activeProfile } = store.getters;
  if (activeProfile) {
    await wsTimeoutWrapper({
      type: 'profiles.remove',
      uuid: activeProfile.uuid,
    });
  }

  // No profile found, assume success
  return true;
};

// 🚀
export const preLaunchChecksValid = async (): Promise<LaunchCheckResult> => {
  const { 'core/getProfiles': profiles, 'core/getActiveProfile': activeProfile } = store.getters;

  if (profiles.length === 0) {
    return {
      ok: false,
      requiresSignIn: true,
      quite: true,
      error: createError('ftb-auth#1000'),
    };
  }

  let profile: AuthProfile | null = profiles.find((profile: AuthProfile) => profile.uuid == activeProfile.uuid);
  if (!profile) {
    return {
      ok: false,
      requiresSignIn: true,
      quite: true,
      error: createError('ftb-auth#1001'),
    };
  }

  // TODO: remove in April 2022
  // if (dayjs().isAfter('2022-03-10')) {
  //   await attemptToRemoveMojangAccounts(profiles, profile, signInAgain);
  // }
  // TODO: end of remove in April 2022

  const validator = profile.type === 'microsoft' ? msAuthenticator : mcAuthenticator;

  logAuth(
    'debug',
    `Validating profile ${profile.username} with uuid ${profile.uuid} against ${
      profile.type === 'microsoft' ? 'Microsoft' : 'Mojang'
    }`,
  );

  const isValid = await wsTimeoutWrapper({
    type: 'profiles.is-valid',
    profileUuid: profile.uuid,
  });

  logAuth('debug', `The users token is ${isValid.success ? 'valid' : 'invalid'}`);
  if (!isValid?.success) {
    logAuth('debug', `Found a profile that no longer can be validated. Trying to refresh`);
    const refresh = await validator.refresh(profile);

    // Update the profiles
    await store.dispatch('core/loadProfiles');
    logAuth('debug', `The refresh was ${refresh ? 'successful' : 'unsuccessful'}`);
    return {
      ok: refresh,
      requiresSignIn: !refresh,
      error: refresh ? createError('ftb-auth#1002') : undefined,
    };
  }

  logAuth('debug', `Successfully authenticated ${profile.username} with uuid ${profile.uuid}`);
  return {
    ok: true,
    requiresSignIn: false,
  };
};

/**
 * Runs the above validation, logs any errors, and handles the removal of the profile if it's invalid / errored and
 * will force the displaying of the login modal if needed.
 *
 * @param instanceId
 */
export const validateAuthenticationOrSignIn = async (instanceId?: string): Promise<boolean> => {
  const validationResult = await preLaunchChecksValid();
  if (validationResult.ok) {
    return true;
  }

  if (validationResult.error) {
    logAuth('error', validationResult.error?.code + ': ' + validationResult.error?.message);
  }

  if (!validationResult.quite) {
    await store.dispatch(
      'showAlert',
      {
        title: 'Error!',
        message:
          'Profile validation failed, please login again. If this keeps happening, as for support in our Discord',
        type: 'danger',
      },
      { root: true },
    );
  }

  if (validationResult.requiresSignIn) {
    logAuth('debug', 'Removing active profile and asking the user to sign-in again');
    await removeActiveProfile();
    await store.dispatch('core/openSignIn', { open: true, tryAgainInstanceUuid: instanceId ?? null }, { root: true });
  }

  return false;
};
