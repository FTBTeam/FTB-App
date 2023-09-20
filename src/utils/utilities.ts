import { SettingsState } from '@/modules/settings/types';
import { RootState } from '@/types';
// import { MCProtocol } from '@/modules/servers/types';
// import mcQuery from 'mcping-js';

export const yeetError = async <T>(promise: () => Promise<T>) => {
  try {
    await promise();
  } catch {}
};

export function debounce(func: () => void, wait: number): () => void {
  let timeout: NodeJS.Timeout | undefined;
  return function () {
    clearTimeout(timeout);

    timeout = setTimeout(() => {
      func();
    }, wait);
  };
}

/**
 * @deprecated makes no sense
 */
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

// TODO: We should just use an api for this. It's simpler. 
// export function queryServer(serverInfo: string): Promise<MCProtocol | undefined> {
//   return new Promise((resolve, reject) => {
//     if (serverInfo.includes(':')) {
//       const address = serverInfo.split(':');
//       const serverIP = address[0];
//       const serverPort = address[1];
//
//       const query = new mcQuery.MinecraftServer(serverIP, serverPort);
//
//       query.ping(5000, (err: any, res: any) => {
//         if (err) {
//           reject(err);
//           return;
//         }
//
//         resolve(res);
//       });
//     } else {
//       const query = new mcQuery.MinecraftServer(serverInfo);
//       query.ping(5000, 5, (err: any, res: any) => {
//         if (err) {
//           reject(err);
//           return;
//         }
//         resolve(res);
//       });
//     }
//   });
// }