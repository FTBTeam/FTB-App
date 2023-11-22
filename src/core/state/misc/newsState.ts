import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {JavaFetch} from '@/core/javaFetch';
import {BlogPost} from '@/core/@types/external/metaApi.types';
import {constants} from '@/core/constants';

export type NewsState = typeof state;

const state = {
  news: [] as BlogPost[],
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
    const newsReq = await JavaFetch.create(`${constants.metaApi}/v1/blog/posts`)
      .execute();
    
    if (!newsReq) {
      commit('SET_LOADING', false);
      return false;
    }
    
    const news = newsReq.json<{posts: BlogPost[]}>();
    commit('SET_NEWS', news.posts);
    commit('SET_LOADING', false);
    return true;
  },
}

const mutations: MutationTree<NewsState> = {
  SET_LOADING: (state: NewsState, loading: boolean) => state.state.loading = loading,
  SET_NEWS: (state: NewsState, news: BlogPost[]) => state.news = news,
}

const getters: GetterTree<NewsState, RootState> = {
  news: (state: NewsState): BlogPost[] => state.news,
  loading: (state: NewsState): boolean => state.state.loading,
}

export const newsStateModule: Module<NewsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}