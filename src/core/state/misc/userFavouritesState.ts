import {RootState} from '@/types';
import {PackProviders} from '@/modules/modpacks/types';

export type UserFavouritesState = ReturnType<typeof state>;

export type UserFavorite = {
  packId: string;
  provider: PackProviders;
  note?: string;
}

const state = () => {
  const userFavourites = localStorage.getItem("userFavourites");
  
  return {
    favourites: (userFavourites ? JSON.parse(userFavourites) : []) as UserFavorite[]
  }
}

const actions: ActionTree<UserFavouritesState, RootState> = {
  async addFavourite({state, commit}, payload: UserFavorite) {
    const newFavs = [...state.favourites, payload];
    commit('SET_FAVOURITES', newFavs);
  },
  async removeFavourite({state, commit}, fav: UserFavorite) {
    const newFavs = state.favourites.filter(f => f.packId !== fav.packId);
    commit('SET_FAVOURITES', newFavs);
  },
  async removeAllFavourites({commit}) {
    commit('SET_FAVOURITES', []);
  }
}

const mutations: MutationTree<UserFavouritesState> = {
  SET_FAVOURITES: (state: UserFavouritesState, favourites: UserFavorite[]) => {
    state.favourites = favourites;
    localStorage.setItem("userFavourites", JSON.stringify(favourites));
  },
}

const getters: GetterTree<UserFavouritesState, RootState> = {
  favourites: (state: UserFavouritesState): UserFavorite[] => state.favourites,
}

export const userFavouritesModule: Module<UserFavouritesState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}