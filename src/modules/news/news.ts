import { Module } from 'vuex';
import { RootState } from '@/types';
import { NewsItem, NewsMutations, NewsState } from '@/modules/news/news.types';
import Parser from 'rss-parser';

const parser: Parser = new Parser();

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
  },
  actions: {
    async fetchNews({ commit }): Promise<void> {
      commit(NewsMutations.NEWS_LOADING);

      try {
        const feed = await parser.parseURL(
          'https://forum.feed-the-beast.com/forum/feed-the-beast-news.35/index.rss?order=post_date',
        );

        // Verify that the feed is valid
        if (feed === undefined || feed.items === undefined) {
          commit(NewsMutations.NEWS_ERROR);
          return;
        }

        // Remap the news feed items to a cleaner format for the news page
        const items = feed.items.reduce((acc: NewsItem[], item: any) => {
          // Replace and links with a clickable link
          const content: string = item['content:encoded'].replace(
            /<a\b[^>]* class="link link--internal">(.*?)<\/a>/gi,
            '',
          );

          acc.push({
            title: item.title || '',
            content: content || '',
            date: item.pubDate || '',
            link: item.link || '',
          });

          return acc;
        }, []);

        // Update the news state
        commit(NewsMutations.NEWS_LOADED, items);
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
