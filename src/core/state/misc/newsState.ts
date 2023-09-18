import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {JavaFetch} from '@/core/javaFetch';

export type NewsState = typeof state;

const state = {
  news: [] as string[],
  state: {
    loading: false,
  }
}

const actions: ActionTree<NewsState, RootState> = {
  async loadNews({state, commit}) {
    if (state.state.loading) {
      return null;
    }

    commit('SET_LOADING', true);
    const newsReq = await JavaFetch.create(`${process.env.VUE_APP_META_API}/v1/blog/posts`)
      .execute();
    
    if (!newsReq) {
      commit('SET_LOADING', false);
      return false;
    }
    
    const news = (await newsReq.json() as any);
    console.log(news.posts);
    
    commit('SET_NEWS', news.posts);
    commit('SET_LOADING', false);
    return true;
  },
}

const mutations: MutationTree<NewsState> = {
  SET_LOADING: (state: NewsState, loading: boolean) => state.state.loading = loading,
  SET_NEWS: (state: NewsState, news: string[]) => state.news = news,
}

const getters: GetterTree<NewsState, RootState> = {
  news: (state: NewsState): string[] => state.news,
  loading: (state: NewsState): boolean => state.state.loading,
}

export const newsStateModule: Module<NewsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}