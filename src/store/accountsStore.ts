import {defineStore} from 'pinia';
import {sendMessage} from '@/core/websockets/websocketsApi.ts';
import {AuthProfile} from '@/core/types/appTypes.ts';
import {FtbAccountManager} from "@/core/auth/ftbAccountManager.ts";
import {JWTIdTokenData} from "@/core/auth/jwtIdTokenData";

export type FTBAccountData = {
  preferred_username?: string;
  name?: string;
  given_name?: string;
  family_name?: string;
  sub: string;
  email?: string;
  picture?: string;
}

type AccountsState = {
  mcProfiles: AuthProfile[];
  mcActiveProfile: AuthProfile | null;
  ftbAccount: {
    accountData: FTBAccountData;
    idTokenData: JWTIdTokenData | null;
  } | null;
  ftbAccountManager: FtbAccountManager;
  ftbLoggingIn: boolean;
  signInFtbOpen: boolean;
  signInOpen: boolean;
}

export const useAccountsStore = defineStore("accounts", {
  state: (): AccountsState => {
    return {
      mcProfiles: [],
      mcActiveProfile: null,
      signInOpen: false,
      signInFtbOpen: false,
      ftbAccount: null,
      ftbLoggingIn: false,
      ftbAccountManager: new FtbAccountManager(),
    }
  },

  getters: {
    isPatreon(s) {
      return s.ftbAccount && Object.values(s.ftbAccount.idTokenData?.resource_access ?? {}).flatMap(e => e.roles).includes("patreon:ads")
    }
  },
  
  actions: {
    openSignIn(open = true) {
      this.signInOpen = open;
    },
    openSignInFtb(open = true) {
      this.signInFtbOpen = open;
    },
    async loadProfiles() {
      const result = await sendMessage("profiles.get", {});

      const profiles = result.profiles.map(
        (a) => ({username: a.minecraftName, uuid: a.uuid} as AuthProfile),
      );
      
      if (result.activeProfile) {
        this.mcActiveProfile = {
          username: result.activeProfile.minecraftName,
          uuid: result.activeProfile.uuid,
        };
      } else {
        this.mcActiveProfile = null;
      }
      
      this.mcProfiles = profiles
    },
    async loadFtbAccount() {
      const accountData = await sendMessage("accounts.get-oauth", {});
      if (!accountData || !accountData.success) {
        // They're just not logged in
        return;
      }
      
      this.ftbAccountManager.init(accountData.authData); 
    },
    async signOutFtb() {
      this.ftbAccountManager.signOut();
      this.ftbAccount = null;
      this.ftbLoggingIn = false;
      
      await sendMessage("accounts.sign-out", {});
    }
  }
})