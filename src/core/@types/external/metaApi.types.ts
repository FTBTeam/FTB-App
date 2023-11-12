export interface Authors {
  id: string;
  slug: string;
  name: string;
  profile_image: string;
}

export interface Primary_author {
  id: string;
  slug: string;
  name: string;
  profile_image: string;
}

export interface BlogPost {
  id: string;
  uuid: string;
  slug: string;
  title: string;
  feature_image: string;
  featured: boolean;
  published_at: string;
  custom_excerpt?: any;
  codeinjection_head: string;
  codeinjection_foot?: any;
  tags: any[];
  authors: Authors[];
  primary_author: Primary_author;
  primary_tag?: any;
  excerpt: string;
  reading_time: number;
  feature_image_alt?: any;
  feature_image_caption?: any;
}

interface AdBase {
  id: string;
  priority: number;
  hover?: string;
  link?: string;
}

type VideoAd = AdBase & {
  type: 'video';
  asset: string;
}

type ImageAd = AdBase & {
  type: 'image';
  asset: string;
}

type TextAd = AdBase & {
  type: 'text';
  value: string;
  markdown: boolean;
}

export type Ad = VideoAd | ImageAd | TextAd;
