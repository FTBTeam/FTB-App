import {AuthProfile} from '@/modules/core/core.types';
import store from '@/modules/store';
import dayjs from 'dayjs';
import {createError} from '@/core/errors/errorCodes';
import {alertController} from '@/core/controllers/alertController';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {consoleBadButNoLogger} from '@/utils';

type RefreshResponse = {
  ok: boolean;
  networkError?: boolean;
  tryLoginAgain?: boolean;
};

interface Authenticator {
  refresh: (profile: AuthProfile) => Promise<RefreshResponse>;
}

export async function loginWithMicrosoft(payload: string | {key: string; iv: string; password: string}): Promise<{ success: boolean; code: string, networkError: boolean }> {
  const responseRaw: any = await fetch('https://msauth.feed-the-beast.com/v1/retrieve', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(typeof payload === "string" ? {credentials: payload} : payload),
  });

  const response: any = (await responseRaw.json()).data;
  if (!response || !response.liveAccessToken || !response.liveRefreshToken || !response.liveExpires) {
    return {
      success: false,
      code: "ftb-f-auth-000001",
      networkError: false,
    };
  }
    
  const res = await sendMessage("profiles.ms.authenticate", {
    ...response
  })

  return {
    success: res.success ?? false,
    code: res.code ?? "ftb-f-auth-000002",
    networkError: res.networkError ?? false
  }
}

export const logAuth = (level: 'debug' | 'warn' | 'error', message: any) =>
  consoleBadButNoLogger("D", `[${dayjs().format('DD/MM/YY hh:mm:ss')}] [${level}] [auth] ${message}`);

const msAuthenticator: Authenticator = {
  async refresh(profile: AuthProfile): Promise<RefreshResponse> {
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

        const authenticator = await sendMessage("profiles.refresh", {
          profileUuid: profile.uuid,
          ...data,
        })

        if (authenticator?.success) {
          logAuth('debug', `Successfully refreshed the token for ${profile.username}`);
          return { ok: true };
        } else {
          logAuth(
            'warn',
            `Failed to refresh the token for ${profile.username}... Error code ${authenticator?.code ?? 'unknown'}`,
          );
          return { ok: false, networkError: authenticator?.networkError ?? false };
        }
      } else {
        logAuth('error', `No encryption details, we can not proceed...`);
        consoleBadButNoLogger("D", 'Unable to refresh token due to missing encryption details', response);
        if (response?.raw?.issue?.error === "invalid_grant") {
          return {ok: false, tryLoginAgain: true};
        }
        
        return {ok: false}
      }
    } catch (e) {
      logAuth('error', `Request errored with the response of ${(e as any).message}`);
      consoleBadButNoLogger("E", e)
      return { ok: false, networkError: true };
    }
  },
};

export type LaunchCheckResult = {
  ok: boolean;
  requiresSignIn: boolean;
  allowOffline?: boolean;
  quite?: boolean;
  error?: {
    message: string;
    code: string;
  };
};

export const removeActiveProfile = async (): Promise<boolean> => {
  const { 'core/getActiveProfile': activeProfile } = store.getters;
  if (activeProfile) {
    await sendMessage("profiles.remove", {
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

  if (profile.type !== "microsoft") {
    return {
      ok: false,
      requiresSignIn: true,
      quite: true,
      error: createError('ftb-auth#1003'),
    };
  }

  logAuth(
    'debug',
    `Validating profile ${profile.username} with uuid ${profile.uuid} against Microsoft`,
  );

  const isValid = await sendMessage("profiles.is-valid", {
    profileUuid: profile.uuid,
  });

  logAuth('debug', `The users token is ${isValid.success ? 'valid' : 'invalid'}`);
  if (!isValid?.success) {
    logAuth('debug', `Found a profile that no longer can be validated. Trying to refresh`);
    const refresh = await msAuthenticator.refresh(profile);

    // Update the profiles
    await store.dispatch('core/loadProfiles');
    logAuth('debug', `The refresh was ${refresh.ok ? 'successful' : 'unsuccessful'}`);
    return {
      ok: refresh.ok,
      allowOffline: !refresh.ok && !refresh.tryLoginAgain,
      requiresSignIn: refresh.tryLoginAgain ?? false,
      error: refresh.ok ? createError('ftb-auth#1002') : undefined,
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
export const validateAuthenticationOrSignIn = async (instanceId?: string): Promise<RefreshResponse> => {
  const validationResult = await preLaunchChecksValid();
  if (validationResult.ok) {
    return {
      ok: true,
    };
  }

  if (validationResult.error) {
    logAuth('error', validationResult.error?.code + ': ' + validationResult.error?.message);
  }

  if (!validationResult.quite) {
    alertController.error(validationResult.requiresSignIn
      ? "We've been unable to refresh your account details, please sign back in"
      : 'Profile validation failed, please login again. If this keeps happening, ask for support in our Discord',)
  }

  if (validationResult.requiresSignIn && !validationResult.allowOffline) {
    logAuth('debug', 'Removing active profile and asking the user to sign-in again');
    await removeActiveProfile();
    await store.dispatch('core/openSignIn', { open: true, tryAgainInstanceUuid: instanceId ?? null }, { root: true });
  }

  return {
    ok: false,
    networkError: validationResult.allowOffline,
  };
};
