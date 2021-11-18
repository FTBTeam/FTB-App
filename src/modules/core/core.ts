import { Module } from 'vuex';
import { RootState } from '@/types';
import { AuthProfile, CoreMutations, CoreState } from '@/modules/core/core.types';

/**
 * Vuex, the correct way.
 */
export const core: Module<CoreState, RootState> = {
  namespaced: true,
  state: {
    profiles: [],
  },
  getters: {
    /**
     * Get the profile from the active profile
     */
    getProfile: (state: CoreState): AuthProfile | null => {
      if (!state.activeProfile) {
        return null;
      }

      return state.profiles.filter(e => e.id === state.activeProfile)[0] ?? null;
    },

    /**
     * Get the active profile
     */
    getActiveProfile: (state: CoreState): string | undefined => {
      return state.activeProfile;
    },
  },
  actions: {
    // TODO: add validation on profile
    addProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.ADD_PROFILE, profile);
    },
  },
  mutations: {
    /**
     * Push a profile onto the state for persistence
     */
    addProfile: (state: CoreState, profile: AuthProfile) => {
      state.profiles.push(profile);
    },
  },
};
