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
}

export const useAccountsStore = defineStore("accounts", {
  state: (): AccountsState => {
    return {
      mcProfiles: [],
      mcActiveProfile: null,
      activeSignInModal: null,
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
    openSignIn(provider: AccountProvider | null = 'MICROSOFT') {
      this.activeSignInModal = provider;
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