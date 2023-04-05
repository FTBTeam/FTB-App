export enum NewsMutations {
  NEWS_LOADING = 'newsLoading',
  NEWS_LOADED = 'newsLoaded',
  NEWS_ERROR = 'newsError',
}

export interface NewsItem {
  title: string;
  url: string;
  createdAt: number;
  postData: string;
  views: number;
}

export interface NewsState {
  news: NewsItem[];
  error: boolean;
  loading: boolean;
}
