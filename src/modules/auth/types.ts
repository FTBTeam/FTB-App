export interface TokenData {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    attributes: {displayName: string; modpackschkey: string; modpackssecret: string};
    mc: McData;
    autoopenchat: boolean;
    accounts: [{identityProvider: string; userId?: string; userName?: string}];
    activePlan: any | null;
}

export interface HashData {
    long: string;
}

export interface ChatData {
    hash: {medium: string; short: string};
    online: boolean;
}

export interface McData {
    hash: HashData;
    chat: ChatData;
    display: string;
    premium: boolean;
    friendCode: string;
}

// oauth -> creates the key -> stores that info into MineTogether -> sends to app backend

export interface AuthState {
    token: TokenData | null;
    session: string;
    error: boolean;
    loading: boolean;
    friends: Friend[];
    isFriendsWindowOpen: boolean;
    loggingIn: boolean;
}

export interface Profile {
    hash: {long: string};
    friendCode: string;
    chat: {hash: {medium: string, short: string}, online: boolean};
    display: string;
    premium: boolean;
}

export interface Friend {
    id?: number;
    hash?: string;
    accepted: boolean;
    name: string;
    online?: boolean;
    currentPack?: string;
    currentPackID?: string;
    notifications?: number;
    currentServer?: number;
    profile: Profile;
}
