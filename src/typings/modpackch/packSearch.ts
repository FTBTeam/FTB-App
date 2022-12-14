export interface ListPackSearchResults {
  packs: PackSearchEntries.Packs[];
  total: number;
  limit: number;
  refreshed: number;
}

export interface NoPackSearchResults {
  status: string;
  message: string;
}

export namespace PackSearchEntries {
  export interface Packs {
    platform: string;
    name: string;
    art: Art[];
    authors: Authors[];
    tags: Tags[];
    synopsis: string;
    id: number;
    updated: number;
    private: boolean;
  }

  export interface Art {
    width: number;
    height: number;
    compressed: boolean;
    url: string;
    size: number;
    id: number;
    type: string;
  }

  export interface Authors {
    website: string;
    id: number;
    name: string;
    type: string;
    updated: number;
  }

  export interface Tags {
    id: number;
    name: string;
  }
}
