import {HttpMethod} from '@/core/@types/commonTypes';
import {JavaFetch} from '@/core/javaFetch';

export class ApiEndpoint {
  constructor(
    private readonly base: string
  ) {
  }

  private async fetch<T>(method: HttpMethod, fetcher: (endpoint: string) => JavaFetch, endpoint: string) {
    if (endpoint.startsWith("http")) {
      throw new Error("Endpoint must not start with http");
    }
    
    if (endpoint.startsWith("/")) {
      endpoint = endpoint.substring(1);
    }
    
    const url = `${this.base}/${endpoint}`;
    const req = await fetcher(url)
      .method(method)
      .execute();
    
    if (req === null) {
      return null;
    }
    
    return req.json<T>();
  }
  
  public async fetchPublic<T>(method: HttpMethod, endpoint: string) {
    return this.fetch<T>(method, JavaFetch.modpacksCh, endpoint);
  }
  
  public async fetchPrivate<T>(method: HttpMethod, endpoint: string) {
    return this.fetch<T>(method, JavaFetch.modpacksChPrivate, endpoint);
  }
}