import {Module} from 'vuex';
import {RootState} from '@/types';
import {AuthProfile, CoreMutations, CoreState} from '@/modules/core/core.types';
import {sendMessage} from '@/core/websockets/websocketsApi';

/**
 * Vuex, the correct way.
 */
export const core: Module<CoreState, RootState> = {
  namespaced: true,
  state: () => {
    let disableAdAside = false;
    const localEntry = localStorage.getItem("debug-disable-ad-aside");
    if (localEntry !== null && localEntry === "true") {
      disableAdAside = true;
    }
    
    return {
      profiles: [],
      activeProfile: {} as AuthProfile,
      signInOpened: { open: false, jumpToAuth: null, uuid: null },
      instanceLoading: false,
      debugDisableAdAside: disableAdAside,
    }
  },
  getters: {
    /**
     * Get the profile from the active profile
     */
    getProfiles: (state: CoreState): AuthProfile[] => {
      return state.profiles;
    },

    /**
     * Get the active profile
     */
    getActiveProfile: (state: CoreState): AuthProfile | undefined => {
      return state.activeProfile;
    },

    /**
     * Sign in opened?
     */
    getSignInOpened: (state: CoreState): { open: boolean; jumpToAuth?: 'ms' | 'mc' | null; uuid?: string | null } => {
      return state.signInOpened;
    },

    /**
     * Instance loading?
     */
    getInstanceLoading: (state: CoreState): boolean => {
      return state.instanceLoading;
    },
    
    getDebugDisabledAdAside: state => process.env.NODE_ENV !== "production" && state.debugDisableAdAside,
  },
  actions: {
    openSignIn: (
      { commit, state },
      payload: { open: boolean; jumpToAuth: 'ms' | 'mc' | null; uuid: string | null; tryAgainInstanceUuid: any },
    ) => {
      commit(CoreMutations.OPEN_SIGNIN, payload);
    },

    closeSignIn: ({ commit }: { commit: any }) => {
      commit(CoreMutations.OPEN_SIGNIN, { open: false, jumpToAuth: null, uuid: null });
    },

    startInstanceLoading: ({ commit }: { commit: any }) => {
      commit(CoreMutations.INSTANCE_LOADING, true);
    },

    stopInstanceLoading: ({ commit }: { commit: any }) => {
      commit(CoreMutations.INSTANCE_LOADING, false);
    },

    addProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.ADD_PROFILE, profile);
    },

    removeProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.REMOVE_PROFILE, profile);
    },

    loadProfiles: async ({ commit }) => {
      const result = await sendMessage("profiles.get", {});
      
      const profiles = result.profiles.map(
        (a: any) =>
          ({
            type: 'microsoft',
            tokens: {
              accessToken: a.msAuth.minecraftToken,
              refreshToken: a.msAuth.liveRefreshToken,
            },
            username: a.username,
            uuid: a.uuid,
            expiresAt: a.isMicrosoft ? a.msAuth.liveExpiresAt : undefined,
          } as AuthProfile),
      );

      commit(CoreMutations.SET_ACTIVE_PROFILE, result.activeProfile);
      commit(CoreMutations.LOAD_PROFILES, profiles);
    },

    toggleDebugDisableAdAside({commit, state}) {
      commit(CoreMutations.TOGGLE_DEBUG_AD_ASIDE);
      localStorage.setItem("debug-disable-ad-aside", "" + state.debugDisableAdAside);
    }
  },
  mutations: {
    openSignIn(
      state: CoreState,
      payload: { open: boolean; jumpToAuth?: 'ms' | 'mc' | null; uuid: string | null; tryAgainInstanceUuid: any },
    ) {
      state.signInOpened = payload;
    },

    instanceLoading(state: CoreState, payload: boolean) {
      state.instanceLoading = payload;
    },

    loadProfiles: (state: CoreState, profiles: AuthProfile[]) => {
      state.profiles = profiles;
    },

    /**
     * Push a profile onto the state for persistence
     */
    addProfile: (state: CoreState, profile: AuthProfile) => {
      if (!state.profiles) {
        state.profiles = [];
      }

      state.profiles.push(profile);
    },

    removeProfile: (state: CoreState, profile: AuthProfile) => {
      state.profiles = state.profiles.filter((p: AuthProfile) => p.uuid !== profile.uuid);
    },

    setActiveProfile: (state: CoreState, profile: AuthProfile) => {
      state.activeProfile = profile;
    },
    
    toggleDebugDisableAdAside: (state) => state.debugDisableAdAside = !state.debugDisableAdAside  
  },
};
