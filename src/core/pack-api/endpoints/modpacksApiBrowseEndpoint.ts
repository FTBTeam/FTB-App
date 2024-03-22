import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {HttpMethod} from '@/core/@types/commonTypes';
import {StatusResult} from '@/core/pack-api/modpackApi';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';
import {PackProviders} from '@/modules/modpacks/types';

type BrowseOptions = {
  tag?: number;
  loader?: string;
  version?: string;
  sort: "new" | "updated" | "popular" | "featured";
  page?: number;
}

export class ModpacksApiBrowseEndpoint extends ApiEndpoint {
  private static readonly PLATFORM_ENDPOINT_MAP: Record<PackProviders, string> = {
    'modpacksch': 'modpack',
    'curseforge': 'curseforge',
  }
  
  constructor() {
    super("");
  }
  
  async tags(platform: PackProviders = 'modpacksch') {
    return this.fetchByPlatform<{
      tags: {id: number, name: string}[]
    }>(platform, "GET", this.computeUrl(platform, 'tags'));
  }
  
  async modTags() {
    return this.fetchPublic<{
      tags: {id: number, name: string}[]
    }>("GET", "curseforge/mods/tags");
  }
  
  async browse(platform: PackProviders = "modpacksch", options: BrowseOptions) {
    // Construct the url based on the options
    const url = this.computeBrowseUrl(platform, options);
    return this.fetchByPlatform<StatusResult<{
      packs: SearchResultPack[],
      page: number,
      pages: number,
    }>>(platform, "GET", url);
  }
  
  async browseMods(options: BrowseOptions) {
    // Construct the url based on the options
    const url = this.computeBrowseUrl("curseforge", options, true);
    return this.fetchPublic<StatusResult<{
      mods: SearchResultPack[],
      page: number,
      pages: number,
    }>>("GET", url);
  }
  
  private computeBrowseUrl(platform: PackProviders, options: BrowseOptions, isMods = false) {
    let suffix = `${isMods ? 'mods/' : ''}browse/`;
    if (options.tag) {
      suffix += `${options.tag}/`;
    }
    
    if (options.loader) {
      suffix += `${options.loader}/`;
    }
    
    if (options.version) {
      suffix += `${options.version}/`;
    }
    
    suffix += `${options.sort}/`;
    
    if (options.page) {
      suffix += `${options.page}`;
    }
    
    return this.computeUrl(platform, suffix);
  }
  
  private computeUrl(platform: PackProviders, suffix: string) {
    const endpoint = ModpacksApiBrowseEndpoint.PLATFORM_ENDPOINT_MAP[platform];
    return `${endpoint}/${suffix}`;
  }
  
  private fetchByPlatform<T>(platform: PackProviders, method: HttpMethod, url: string) {
    return platform === "modpacksch" ? this.fetchPrivate<T>(method, url) : this.fetchPublic<T>(method, url);
  }
}