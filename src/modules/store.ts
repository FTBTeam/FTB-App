import {RootState} from '@/types';
// import {websocket} from './websocket';
import {settings} from './settings';
import {core} from '@/modules/core/core';
import {modpackStateModule} from '@/core/state/modpacks/modpacksState';
import {instanceStateModule} from '@/core/state/instances/instancesState';
import {installStateModule} from '@/core/state/instances/installState';
import {dialogsState} from '@/core/state/misc/dialogsState';

export const mutations: MutationTree<RootState> = {
  HIDE_MODAL(state: any) {
    state.websocket.modal = null;
  },
};

const store: StoreOptions<RootState> = {
  state: {} as any,
  actions: {
    hideModal: ({ commit }: any) => {
      commit('HIDE_MODAL');
    },
  },
  mutations,
  modules: {
    core,
    // websocket,
    settings,
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
    "v2/install": installStateModule,
    "v2/dialogs": dialogsState,
  }
};

export default store;

// export default new Vuex.Store<RootState>(store);
