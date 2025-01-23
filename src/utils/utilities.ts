import {Socket} from '@/modules/websocket/types';
import {createLogger} from '@/core/logger';
import {SettingsData} from '@/core/@types/javaApi';

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

export function adsEnabled(settings: SettingsData, debugMode = false) {
  if (process.env.NODE_ENV !== "production" && debugMode) {
    return false
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

export async function waitForWebsocketsAndData(user: string, websockets: Socket, fieldDataPredicate: () => boolean) {
  logger.debug("Waiting for websockets and data for user", user);
  await waitForWebsockets(user, websockets);
  
  logger.debug("Websockets connected, waiting for data for user", user);
  // Now wait for some data, let's give it 30 seconds to get the data before we give up
  await new Promise((resolve, reject) => {
    const timeoutRef = setTimeout(() => {
      clearTimeout(timeoutRef);
      reject("Timed out waiting for data");
    }, 30_000);
    
    const interval = setInterval(() => {
      if (fieldDataPredicate()) {
        logger.debug("Data found for user", user);
        clearInterval(interval);
        clearTimeout(timeoutRef);
        resolve(null);
      }
    }, 100);
  });
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