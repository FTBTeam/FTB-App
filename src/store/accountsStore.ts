import { defineStore } from 'pinia';
import { sendMessage } from '@/core/websockets/websocketsApi.ts';
import { AuthProfile } from '@/core/types/appTypes.ts';

type AccountsState = {
  mcProfiles: AuthProfile[];
  mcActiveProfile: AuthProfile | null;
  ftbAccount: unknown;
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
      }
      
      this.mcProfiles = profiles
    }
  }
})