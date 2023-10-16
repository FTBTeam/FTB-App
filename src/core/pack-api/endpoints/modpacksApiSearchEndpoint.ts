import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {ListPackSearchResults} from '@/core/@types/modpacks/packSearch';

export class ModpacksApiSearchEndpoint extends ApiEndpoint {
  constructor() {
    super("modpack/search");
  }
  
  async search(query: string, platform = "modpacksch", limit = 20) {
    const queryUrl = `/${limit}/detailed/?term=${query}&platform=${platform}`;
    
    if (platform === "modpacksch") {
      return this.fetchPrivate<ListPackSearchResults>("GET", queryUrl);
    } else {
      return this.fetchPublic<ListPackSearchResults>("GET", queryUrl);
    }
  }
}