import { SettingsState } from './modules/settings/types';
import { RootState } from '@/types';
import {MCProtocol} from '@/modules/servers/types';
// @ts-ignore
import mcQuery from 'mcping-js';

export function debounce(func: () => void, wait: number): () => void {
    let timeout: number | undefined;
    return function() {
         clearTimeout(timeout);
         // @ts-ignore
         timeout = setTimeout(() => {
            // @ts-ignore
             func.apply(this, arguments);
         }, wait);
    };
}

export async function asyncForEach(items: any[], callback: (item: any) => Promise<any>): Promise<any> {
    for (let i = 0; i < items.length; i++) {
        await callback(items[i]);
    }
}

export function logVerbose(state: RootState | SettingsState, ...message: any[]) {
    // @ts-ignore
    if (state.settings?.settings === undefined) {
        // @ts-ignore
        if (state.settings?.verbose === true || state.settings?.verbose === 'true') {
            console.log('[DEBUG]', ...message);
        }
    } else {
        // @ts-ignore
        if (state.settings?.settings.verbose === true || state.settings?.settings.verbose === 'true') {
            console.log('[DEBUG]', ...message);
        }
    }
}

export function shortenHash(longHash: string): string {
    return `MT${longHash.substring(0, 28).toUpperCase()}`;
}

export function shuffle(array: any[]) {
    // @ts-ignore
    let i = array.length, j, temp;
    if ( i === 0 ) { return array; }
    while ( --i ) {
       j = Math.floor( Math.random() * ( i + 1 ) );
       temp = array[i];
       array[i] = array[j];
       array[j] = temp;
    }
    return array;
}

export function queryServer(serverInfo: string): Promise<MCProtocol | undefined> {
    return new Promise((resolve, reject) => {
        if (serverInfo.includes(':')) {
            const address = serverInfo.split(':');
            const serverIP = address[0];
            const serverPort = address[1];

            const query = new mcQuery.MinecraftServer(serverIP, serverPort);

            query.ping(10000, async (err: any, res: any) => {
                if (err) {
                    console.error(err);
                    reject(err);
                    return undefined;
                }
                resolve(res);
            });
        } else {
            const query = new mcQuery.MinecraftServer(serverInfo);
            query.ping(10000, 5, async (err: any, res: any) => {
                if (err) {
                    console.error(err);
                    reject(err);
                    return undefined;
                }
                resolve(res);
            });
        }
    });
}
