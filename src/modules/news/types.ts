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
