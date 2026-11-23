import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {ListPackSearchResults} from '@/core/types/modpacks/packSearch';
import {Mod} from '@/types';
import {StatusResult} from '@/core/pack-api/modpackApi';

export class ModpacksApiSearchEndpoint extends ApiEndpoint {
  constructor() {
    super("");
  }
  
  async search(query: string, platform = "ftb", limit = 20) {
    const queryUrl = `modpack/search/${limit}/detailed/?term=${query}&platform=${platform}`;
    
    if (platform === "ftb") {
      return this.fetch<ListPackSearchResults>("GET", queryUrl);
    } else {
      return this.fetch<ListPackSearchResults>("GET", queryUrl);
    }
  }
  
  async modSearch(query: string, mcVersion: string, loader: "forge" | "fabric" | "neoforge" | "all", limit = 50) {
    return this.fetch<StatusResult<{mods: number[], total: number}>>("GET", `mod/search/${mcVersion}/${loader}/${limit}?term=${query}`);
  }
  
  async modFetch(id: number) {
    return this.fetch<Mod>("GET", `mod/${id}`);
  }
}