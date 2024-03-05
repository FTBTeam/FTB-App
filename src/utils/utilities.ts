import {Socket} from '@/modules/websocket/types';
import {createLogger} from '@/core/logger';
import {MineTogetherAccount, SettingsData} from '@/core/@types/javaApi';

const logger = createLogger("utils/utilities.ts");

export function debounce(func: () => void, wait: number): () => void {
  let timeout: NodeJS.Timeout | undefined;
  return function () {
    clearTimeout(timeout);

    timeout = setTimeout(() => {
      func();
    }, wait);
  };
}

export function shortenHash(longHash: string): string {
  return `MT${longHash.substring(0, 28).toUpperCase()}`;
}

export function adsEnabled(settings: SettingsData, mtAccount: MineTogetherAccount | null, debugMode = false) {
  if (process.env.NODE_ENV !== "production" && debugMode) {
    return false
  }
  
  if (!mtAccount || !mtAccount?.activePlan || mtAccount.activePlan.status !== "Active") {
    return true;
  }
  
  // If this fails, show the ads
  return settings?.appearance?.showAds ?? true;
}

export async function waitForWebsockets(user: string, websockets: Socket) {
  if (!websockets.isConnected) {
    logger.debug("WS not connected, delaying page load for 100ms for user", user);
    await new Promise((resolve) => {
      // Wait for 30 seconds for the websocket to connect
      const timoutRef = setTimeout(() => {
        clearTimeout(timoutRef);
        resolve(null);
      }, 30_000); // 30 seconds
      
      // Check every 100ms if the websocket is connected
      const interval = setInterval(() => {
        if (websockets.isConnected) {
          clearInterval(interval);
          resolve(null);
        }
      }, 100);
    });
  }
}

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