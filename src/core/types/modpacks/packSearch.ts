import {Art, Authors, ModPackTag} from '@/modules/modpacks/types';

export interface ListPackSearchResults {
  packs: SearchResultPack[];
  total: number;
  limit: number;
  refreshed: number;
}

export interface NoPackSearchResults {
  status: string;
  message: string;
}

export interface SearchResultPack {
  platform: string;
  name: string;
  art: Art[];
  authors: Authors[];
  tags: ModPackTag[];
  synopsis: string;
  id: number;
  updated: number;
  private: boolean;
}
