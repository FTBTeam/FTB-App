import {MutationTree} from 'vuex';
import {NewsState, NewsItem} from './types';

export const mutations: MutationTree<NewsState> = {
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
};
