import {ActionTree} from 'vuex';
import { NewsState, NewsItem } from './types';
import {RootState} from '@/types';
import Parser from 'rss-parser';

const parser: Parser = new Parser();

export const actions: ActionTree<NewsState, RootState> = {
    fetchNews({commit}): any {
        commit('newsLoading');
        parser.parseURL('https://forum.feed-the-beast.com/forum/feed-the-beast-news.35/index.rss?order=post_date').then((feed) => {
            if (feed === undefined || feed.items === undefined) {
                commit('newsError');
                return;
            }
            const payload: NewsItem[] = [];
            feed.items.forEach((item) => {
                // const content: string = item['content:encoded'].replace(/<a\b[^>]*>(.*?)<\/a>/gi, '');
                const content: string = item['content:encoded'].replace(/<a\b[^>]* class="link link--internal">(.*?)<\/a>/gi, '');
                payload.push({
                    title: item.title || '',
                    content: content || '',
                    date: item.pubDate || '',
                    link: item.link || '',
                });
            });
            commit('newsLoaded', payload);
        });
    },
};
