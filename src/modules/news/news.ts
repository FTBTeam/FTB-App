import { Module } from 'vuex';
import { RootState } from '@/types';
import { NewsItem, NewsMutations, NewsState } from '@/modules/news/news.types';
import * as process from 'process';

let metaApi = process.env.VUE_APP_META_API;
if (!metaApi || metaApi === '' || !metaApi.startsWith("https")) {
  metaApi = "https://meta.feed-the-beast.com";
}

/**
 * Vuex store for the news section of the app.
 */
export const news: Module<NewsState, RootState> = {
  namespaced: true,
  state: {
    news: [],
    loading: false,
    error: false,
  },
  getters: {
    /**
     * Gets the news from the store
     */
    getNews: (state: NewsState): NewsItem[] => {
      return state.news;
    },
    
    newsLoading: (state: NewsState): boolean => {
      return state.loading
    }
  },
  actions: {
    async fetchNews({ commit }): Promise<void> {
      commit(NewsMutations.NEWS_LOADING);

      try {
        const data = await fetch(`${metaApi}/v1/news`);
        
        // Update the news state
        commit(NewsMutations.NEWS_LOADED, await data.json());
      } catch {
        commit(NewsMutations.NEWS_ERROR);
      }
    },
  },
  mutations: {
    newsLoading(state) {
      state.loading = true;
    },
    newsLoaded(state, payload: NewsItem[]) {
      state.error = false;
      state.news = payload;
      state.loading = false;
    },
    newsError(state) {
      state.error = true;
      state.news = [];
      state.loading = false;
    },
  },
};
