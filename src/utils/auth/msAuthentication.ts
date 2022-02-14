import axios, { AxiosResponse } from 'axios';

const issueCodes = {
  I1: '000001',
  I2: '000002',
  I3: '000003',
  I4: '000004',
  I5: '000005',
  I6: '000006',
  I7: '000007',
};

export const finishAuthentication = async (liveAccessToken: string, liveRefreshToken: string, liveExpires: string) => {
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
  if (!ownership.ok) {
    return {
      ok: false,
      code: issueCodes.I5,
    };
  }

  const { items: ownedItems } = ownership.data ?? {};

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
  if (ownedItems?.length === 0 && !minecraftName) {
    return {
      ok: false,
      code: issueCodes.I7,
    };
  }

  return {
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
    liveExpiresAt: Math.round(Date.now() / 1000) + liveExpires,
  };
};

const request = async (requester: () => Promise<AxiosResponse>) => {
  try {
    const res = await requester();
    return {
      ok: true,
      data: res.data,
    };
  } catch (e: any) {
    console.error(e.response);
    return {
      ok: false,
      issue: {
        error: JSON.stringify(e.response.statusMessage),
        code: JSON.stringify(e.response.code),
      },
    };
  }
};

const axiosClient = axios.create();

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Authenticate_with_XBL
 */
const authWithXBL = async (authToken: string) => {
  return request(() =>
    axiosClient.post(`https://user.auth.xboxlive.com/user/authenticate`, {
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
const authWithXSTS = async (xblToken: string) => {
  return request(() =>
    axiosClient.post(
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
const authWithMinecraft = async ({ userHash, xstsToken }: { userHash: string; xstsToken: string }) => {
  return request(() =>
    axiosClient.post(
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
const checkOwnership = async (accessToken: string) => {
  return request(() =>
    axiosClient.get(`https://api.minecraftservices.com/entitlements/mcstore`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }),
  );
};

/**
 * https://wiki.vg/Microsoft_Authentication_Scheme#Get_the_profile
 */
const getMcProfile = async (accessToken: string) => {
  return request(() =>
    axiosClient.get(`https://api.minecraftservices.com/minecraft/profile`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
    }),
  );
};
