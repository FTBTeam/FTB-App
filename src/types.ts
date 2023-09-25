import { SettingsState } from './modules/settings/types';
import { AuthState, Friend } from './modules/auth/types';
import {AppState} from '@/core/state/appState';

export namespace App {
  export interface MsgBox {
    title: string;
    content: string;
    type: string;
    okAction: () => void;
    cancelAction: () => void;
  }
}

export type RootState = {
  version: string;
  wsPort: number;
  wsSecret: string;
  settings: SettingsState | null;
  auth: AuthState | null;
} & AppState


export interface ModalBox {
  title: string;
  message: string;
  buttons: Button[];
  dismissable: boolean;
}

export interface FriendListResponse {
  online: Friend[];
  offline: Friend[];
  pending: Friend[];
}

export interface Button {
  name: string;
  message: string;
  colour: string;
}

export interface Messages {
  [index: string]: Message[];
}

export interface UnreadMessages {
  [index: string]: number;
}

export interface Message {
  content: string;
  author: string;
  date: number;
  read: boolean;
}

declare module 'vue/types/vue' {
  interface Vue {
    $connect: (url?: string, options?: any) => {};
    $socket: any;
    $disconnect: () => {};
  }
  interface VueConstructor {
    _installedPlugins: any;
  }
}

/**
 * This is basically the same as the Vuex type for modpacks but I don't trust that code so here we go again :D
 */
export interface Mod {
  synopsis: string;
  description: string;
  art: ModArt[];
  links: ModLinks[];
  authors: ModAuthor[];
  versions: ModVersion[];
  installs: number;
  status: string;
  id: number;
  name: string;
  type: string;
  updated: number;
  refreshed: number;
}

export interface ModVersion {
  targets: ModTarget[];
  version: string;
  path: string;
  url: string;
  sha1: string;
  size: number;
  clientonly: boolean;
  id: number;
  name: string;
  type: string;
  updated: number;
  dependencies: any[];
}

export interface ModTarget {
  version: string;
  id?: number;
  name: string;
  type: string;
  updated: number;
}

export interface ModAuthor {
  website: string;
  id: number;
  name: string;
  type: string;
  updated: number;
}

export interface ModLinks {
  id: number;
  name: string;
  link: string;
  type: string;
}

export interface ModArt {
  width: number;
  height: number;
  compressed: boolean;
  url: string;
  type: string;
}

export interface ModSearchResults {
  mods: number[];
  total: number;
  limit: number;
  refreshed: number;
}

export interface DiscordWidget {
  id: string;
  name: string;
  instant_invite: string;
  channels: any[];
  members: {
    id: string;
    username: string;
    discriminator: string;
    avatar: null;
    status: "dnd" | "idle" | "online";
    avatar_url: string;
    game?: {
      name: string;
    };
  }[];
  presence_count: number;
}