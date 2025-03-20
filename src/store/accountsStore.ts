import { defineStore } from 'pinia';
import { AuthProfile } from '@/modules/core/core.types.ts';

type AccountsState = {
  mcProfiles: AuthProfile[];
  mcActiveProfile: AuthProfile | null;
  
  signInOpen: boolean;
}

export const useAccountsStore = defineStore("accounts", {
  state: (): AccountsState => {
    return {
      mcProfiles: [],
      mcActiveProfile: null,
      signInOpen: false
    }
  },

  actions: {
    openSignIn(open = true) {
      this.signInOpen = open;
    }
  }
})