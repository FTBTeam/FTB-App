
export interface ServerList {
    [index: number]: Server;
}

export interface ServersState {
    error: boolean;
    loading: boolean;
    servers: ServerList;
}

export interface Location {
    country: string;
    country_code:string;
    city:string;
    continent:string;
    subdivisionlocation_provider: string;
    cached: boolean;
}

export interface Server {
    id?: number;
    name: string;
    invite: boolean;
    community: boolean;
    project: string;
    ip: string;
    api_version: number;
    expected_players: number;
    location: Location;
    featured: boolean;
    uptime: number;
    port: number;
}
