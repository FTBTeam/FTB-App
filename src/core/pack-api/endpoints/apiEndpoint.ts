import {HttpMethod} from '@/core/types/commonTypes';
import {JavaFetch} from '@/core/javaFetch';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';

export class ApiEndpoint {
  constructor(
    private readonly base: string
  ) {
  }

  private async fetch<T>(method: HttpMethod, fetcher: (endpoint: string) => JavaFetch, endpoint: string, baseEndpointOverride?: string) {
    if (endpoint.startsWith("http")) {
      throw new Error("Endpoint must not start with http");
    }
    
    if (endpoint.startsWith("/")) {
      endpoint = endpoint.substring(1);
    }
    
    const base = stringIsEmpty(baseEndpointOverride) && stringIsEmpty(this.base) ? '' : `${baseEndpointOverride ?? this.base}/`;    
    const url = `${base}${endpoint}`;

    const req = await fetcher(url)
      .method(method)
      .execute();
    
    if (req === null) {
      return null;
    }
    
    return req.json<T>();
  }
  
  public async fetchPublic<T>(method: HttpMethod, endpoint: string, baseEndpointOverride?: string) {
    return this.fetch<T>(method, JavaFetch.modpacksCh, endpoint, baseEndpointOverride);
  }
  
  public async fetchPrivate<T>(method: HttpMethod, endpoint: string, baseEndpointOverride?: string) {
    return this.fetch<T>(method, JavaFetch.modpacksChPrivate, endpoint, baseEndpointOverride);
  }
}