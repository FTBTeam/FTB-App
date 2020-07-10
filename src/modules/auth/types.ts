export interface TokenData {
    modpackskey: string;
    modpackssecret: string;
    username: string;
    mc: McData;
}

export interface McData {
    uuid: string;
    hash: string;
    mtusername: string;
    friendCode: string;
}

// oauth -> creates the key -> stores that info into MineTogether -> sends to app backend

export interface AuthState {
    token: TokenData | null;
    error: boolean;
    loading: boolean;
    friends: Friend[];
}

export interface Friend {
    id: number;
    hash: string;
    accepted: boolean;
    name: string;
    online?: boolean;
    currentPack?: string;
    shortHash: string;
    notifications?: number;
}