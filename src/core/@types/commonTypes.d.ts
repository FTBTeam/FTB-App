export type AsyncFunction = (...args: any[]) => Promise<void>;

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'OPTIONS';