import {CompleteTokenData} from "@/core/types/javaApi";
import {FTBAccountData, useAccountsStore} from "@/store/accountsStore.ts";
import {createLogger} from "@/core/logger.ts";
import {getOrCreateOauthClient} from "@/utils";
import * as client from "openid-client";
import dayjs from "dayjs";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import {JWTIdTokenData} from "@/core/auth/jwtIdTokenData";
import {retrying} from "@/utils/helpers/asyncHelpers.ts";


const logger = createLogger('ftbAccountManager.ts');

export class FtbAccountManager {
  private tokenData: CompleteTokenData | null = null;
  private refreshInterval: NodeJS.Timeout | null = null;
  private refreshLock = false;
  
  public async init(tokenData: CompleteTokenData) {
    this.tokenData = tokenData;
    if (this.isExpired) {
      await this.refreshToken();
    }
    
    if (this.isExpired) {
      const accountStore = useAccountsStore();
      accountStore.ftbAccount = null;
      this.tokenData = null;
      return;
    }
    
    this.startRefreshCycle();

    const accountStore = useAccountsStore();
    accountStore.ftbLoggingIn = true;

    logger.log("Logging in to FTB account");
    await this.updateUserData(this.tokenData);

    accountStore.ftbLoggingIn = false;
  }
  
  private startRefreshCycle() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    
    this.refreshInterval = setInterval(() => {
      logger.info("Checking if token needs refreshing token in background");
      this.refreshToken().catch((e) => {
        logger.error("Failed to refresh token", e);
      });
    }, 60 * 1000); // 1 minute
  }
  
  private async refreshToken() {
    if (!this.tokenData || !this.isExpired || this.refreshLock) return;
    this.refreshLock = true;

    logger.info("Refreshing token");
    const config = await getOrCreateOauthClient();
    const newTokenData = await retrying(async () => client.refreshTokenGrant(config, this.tokenData!.refreshToken));
    if (newTokenData?.access_token) {
      const result = await sendMessage("accounts.store-oauth", {
        token: newTokenData.access_token,
        idToken: newTokenData.id_token!,
        refreshToken: newTokenData.refresh_token!,
        expiresIn: newTokenData.expires_in!,
        refreshExpiresIn: newTokenData.refresh_expires_in! as any,
        notViableForLogging: true
      })
      
      if (result.completeToken) {
        logger.info("Successfully refreshed token");
        await this.updateUserData(result.completeToken);
      }
    } else {
      // It no work,
      this.tokenData = null;
      
      const accountStore = useAccountsStore();
      accountStore.ftbAccount = null;
      
      logger.error("Failed to refresh token");
    }
    
    this.refreshLock = false;
  }
  
  private async updateUserData(tokenData: CompleteTokenData | null) {
    const accountStore = useAccountsStore();
    
    this.tokenData = tokenData;
    if (!this.tokenData) {
      accountStore.ftbAccount = null;
      return;
    }
    
    const idToken = this.parseIdToken(this.tokenData.idToken);
    const normalTokenData: any = this.parseToken(this.tokenData.token); // TODO: Types
    
    const sub = idToken?.sub ?? normalTokenData.sub;
    try {
      const userData = await retrying(async () => client.fetchUserInfo(await getOrCreateOauthClient(), this.tokenData!.token, sub));
      if (!userData) {
        return
      }
      
      accountStore.ftbAccount = {
        accountData: userData satisfies FTBAccountData,
        idTokenData: idToken,
      }
    } catch (e) {
      logger.error("Failed to fetch user data", e);
      accountStore.ftbAccount = null;
    }
  }
  
  private parseIdToken(idToken: string) {
    const decoded = atob(idToken.split(".")[1]);
    const parse = JSON.parse(decoded);
    if ("aud" in parse) {
      return parse as JWTIdTokenData
    }
    
    return null;
  }
  
  private parseToken(token: string) {
    const decoded = atob(token.split(".")[1]);
    return JSON.parse(decoded);
  }
  
  get isExpired() {
    if (!this.tokenData) return true;
    
    // The unix timestamp of the expiration date
    const expiresAt = this.tokenData.expiresAt;
    const now = dayjs().unix();
    
    // Give the token a minute less than the actual expiration date
    return (expiresAt - 60) < now;
  }
  
  get isSignedIn() {
    return this.tokenData !== null;
  }
}