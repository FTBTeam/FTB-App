import {Module} from 'vuex';
import {getters} from './getters';
import {actions} from './actions';
import {mutations} from './mutations';
import {ModpackState} from './types';
import {RootState} from '@/types';

export const state: ModpackState = {
    packsCache: {},
    installedPacks: [],
    search: [],
    popularInstalls: [],
    popularPlays: [],
    featuredPacks: [],
    recentPacks: [],
    error: false,
    errorMsg: '',
    currentModpack: null,
    installing: [],
    loading: false,
    searchString: '',
};

export const modpacks: Module<ModpackState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
