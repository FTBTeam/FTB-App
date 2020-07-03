import { ModPack } from './../modpacks/types';
export interface DiscoveryState {
    error: boolean;
    loading: boolean;
    discoveryQueue: ModPack[];
}
