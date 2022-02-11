export enum NewsMutations {
  NEWS_LOADING = 'newsLoading',
  NEWS_LOADED = 'newsLoaded',
  NEWS_ERROR = 'newsError',
}

export interface NewsItem {
  title: string;
  date: string;
  content: string;
  link: string;
}

export interface NewsState {
  news: NewsItem[];
  error: boolean;
  loading: boolean;
}
