import axios, { AxiosResponse } from 'axios';
import qs from 'qs';
import platform from '@/utils/interface/electron-overwolf';
// const Settings = require('./settings');

export const msAuthSettings = {
  CLIENT_ID: 'f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8',
  LIVE_URL: 'https://login.live.com/oauth20_authorize.srf',
  LIVE_REDIRECT: 'https://login.microsoftonline.com/common/oauth2/nativeclient',
};

// TOOD: REMOVE AND FIX
const issueCodes = {
  I1: '000001',
  I2: '000002',
  I3: '000003',
  I4: '000004',
  I5: '000005',
  I6: '000006',
  I7: '000007',
};

export const authenticateMc = async (code: string, verifier: string) => {
  if (!code || !verifier) {
    return {
      failed: 'no code...',
    };
  }

  // Login to MS
  const liveRes = await authWithLive(code, verifier);
  if (!liveRes.ok || !liveRes.data?.access_token) {
    return {
      ok: false,
      code: issueCodes.I1,
    };
  }

  const { access_token: liveAccessToken, refresh_token: liveRefreshToken, expires_in: liveExpires } = liveRes.data;

  // Use the access token from above to log into XBox live
  const xblRes = await authWithXBL(liveAccessToken);
  if (!xblRes.ok || !xblRes.data?.Token) {
    return {
      ok: false,
      code: issueCodes.I2,
    };
  }

  const {
    Token: xblToken,
    DisplayClaims: {
      xui: [{ uhs: xblUserHash }],
    },
  } = xblRes.data;

  // Validate that info and authenticate with XSTS
  const xstsRes = await authWithXSTS(xblToken);
  if (!xstsRes.ok || !xstsRes.data?.Token) {
    return {
      ok: false,
      code: issueCodes.I3,
    };
  }

  const {
    Token: xstsToken,
    DisplayClaims: {
      xui: [{ uhs: xstsUserHash }],
    },
  } = xstsRes.data;

  // Get the minecraft account
  const minecraftRes = await authWithMinecraft({
    userHash: xblUserHash,
    xstsToken: xstsToken,
  });

  if (!minecraftRes.ok || !minecraftRes.data?.access_token) {
    return {
      ok: false,
      code: issueCodes.I4,
    };
  }

  const { access_token: minecraftToken } = minecraftRes.data;

  // Check if they own the game. This can be empty and they can still access the game from the gamepass
  const ownership = await checkOwnership(minecraftToken);
  if (!ownership.ok || !ownership.data?.items) {
    return {
      ok: false,
      code: issueCodes.I5,
    };
  }

  const { items: ownedItems } = ownership.data;

  // Confirm they have a profile and pull data.
  const minecraftProfileRes = await getMcProfile(minecraftToken);
  if (!minecraftProfileRes.ok) {
    return {
      ok: false,
      code: issueCodes.I6,
    };
  }

  const { name: minecraftName, id: minecraftUuid } = minecraftProfileRes.data;
  // They don't own the game :cry:
  if (ownedItems.length === 0 && !minecraftName) {
    return {
      ok: false,
      code: issueCodes.I7,
    };
  }

  const payload = {
    minecraftName,
    minecraftUuid,
    minecraftToken,
    xstsUserHash,
    xstsToken,
    xblUserHash,
    xblToken,
    liveAccessToken,
    liveRefreshToken,
    liveExpires,
  };

  return payload;
};

const request = async <T>(requester: () => Promise<AxiosResponse<T>>): Promise<any> => {
  try {
    const res = await requester();
    return {
      ok: true,
      data: res.data,
    };
  } catch (e) {
    const error: any = e;
    console.error(error.response);
    return {
      ok: false,
      issue: {
        error: JSON.stringify(error.response.statusMessage),
        code: JSON.stringify(error.response.code),
      },
    };
  }
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Authorization_Code_-.3E_Authorization_Token
 */
export const authWithLive = async (code: string, verifier: string, isRefresh = false) => {
  // Now try and get the users auth token
  return request(() =>
    axios.post(
      `https://login.live.com/oauth20_token.srf`,
      qs.stringify({
        grant_type: 'authorization_code',
        client_id: msAuthSettings.CLIENT_ID,
        scope: 'offline_access xboxlive.signin xboxlive.offline_access',
        redirect_uri: platform.isOverwolf() ? 'https://feed-the-beast.com/msauth' : msAuthSettings.LIVE_REDIRECT,
        code,
        code_verifier: verifier,
      }),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-Skip-Origin': 'skip',
        },
      },
    ),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XBL
 */
export const authWithXBL = async (authToken: string) => {
  return request(() =>
    axios.post(`https://user.auth.xboxlive.com/user/authenticate`, {
      Properties: {
        AuthMethod: 'RPS',
        SiteName: 'user.auth.xboxlive.com',
        RpsTicket: `d=${authToken}`,
      },
      RelyingParty: 'http://auth.xboxlive.com',
      TokenType: 'JWT',
    }),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XSTS
 */
export const authWithXSTS = async (xblToken: string) => {
  return request(() =>
    axios.post(
      `https://xsts.auth.xboxlive.com/xsts/authorize`,
      {
        Properties: {
          SandboxId: 'RETAIL',
          UserTokens: [xblToken],
        },
        RelyingParty: 'rp://api.minecraftservices.com/',
        TokenType: 'JWT',
      },
      {
        headers: {
          accept: 'application/json',
        },
      },
    ),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_Minecraft
 */
export const authWithMinecraft = async ({ userHash, xstsToken }: { userHash: string; xstsToken: string }) => {
  return request(() =>
    axios.post(
      `https://api.minecraftservices.com/authentication/login_with_xbox`,
      {
        identityToken: `XBL3.0 x=${userHash};${xstsToken}`,
      },
      {
        headers: {
          accept: 'application/json',
        },
      },
    ),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Checking_Game_Ownership
 */
export const checkOwnership = async (accessToken: string) => {
  return request(() =>
    axios.get(`https://api.minecraftservices.com/entitlements/mcstore`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Get_the_profile
 */
export const getMcProfile = async (accessToken: string) => {
  return request(() =>
    axios.get(`https://api.minecraftservices.com/minecraft/profile`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }),
  );
};
