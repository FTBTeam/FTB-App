import {defineStore} from 'pinia';
import {sendMessage} from '@/core/websockets/websocketsApi.ts';
import {AuthProfile} from '@/core/types/appTypes.ts';
import {InstanceActions} from "@/core/actions/instanceActions.ts";
import {AccountProvider, FtbAccount} from "@/core/types/javaApi";

type AccountsState = {
  mcProfiles: AuthProfile[];
  mcActiveProfile: AuthProfile | null;
  ftbAccounts: FtbAccount[];
  ftbAccount: FtbAccount | null;
  activeSignInModal: AccountProvider | null;
  // Set when signing a known account back in, so the provider can skip asking which account to use
  signInAccountId: string | null;
}

export const useAccountsStore = defineStore("accounts", {
  state: (): AccountsState => {
    return {
      mcProfiles: [],
      mcActiveProfile: null,
      activeSignInModal: null,
      signInAccountId: null,
      ftbAccounts: [],
      ftbAccount: null,
    }
  },

  getters: {
    isPremiumMember(s) {
      return !!s.ftbAccount && !s.ftbAccount.notLoggedIn && s.ftbAccount.premium
    }
  },
  
  actions: {
    openSignIn(provider: AccountProvider | null = 'MICROSOFT', accountId: string | null = null) {
      this.activeSignInModal = provider;
      this.signInAccountId = provider ? accountId : null;
    },
    async loadProfiles() {
      const result = await sendMessage("profiles.get", {});

      const profiles = result.profiles.map(
        (a) => ({username: a.minecraftName, uuid: a.uuid, notLoggedIn: a.notLoggedIn} as AuthProfile),
      );
      
      if (result.activeProfile) {
        this.mcActiveProfile = {
          username: result.activeProfile.minecraftName,
          uuid: result.activeProfile.uuid,
          notLoggedIn: result.activeProfile.notLoggedIn,
        };
      } else {
        this.mcActiveProfile = null;
      }
      
      this.mcProfiles = profiles

      this.ftbAccounts = result.ftbAccounts;
      this.ftbAccount = result.activeFtbAccount ?? null;
    },
    async removeFtbAccount(id: string) {
      const result = await sendMessage("profiles.remove", {provider: "FTB", id});
      await this.ftbAccountChanged();
      return result.success;
    },
    async setActiveFtbAccount(id: string) {
      const result = await sendMessage("profiles.setActiveProfile", {provider: "FTB", id});
      await this.ftbAccountChanged();
      return result.success;
    },
    // Call after an FTB account is added, removed or switched
    async ftbAccountChanged() {
      await this.loadProfiles();
      await InstanceActions.clearInstanceCache(false);
    }
  }
})