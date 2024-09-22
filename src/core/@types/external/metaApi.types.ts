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
