// jacked from: https://github.com/sindresorhus/pretty-bytes
// Jank = rushmead
import store from '@/modules/store';

export const prettyByteFormat = (bytes: number) => {
  if (isNaN(bytes)) {
    throw new TypeError('Expected a number');
  }

  let exponent;
  let unit;
  const neg = bytes < 0;
  const units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

  if (neg) {
    bytes = -bytes;
  }

  if (bytes < 1) {
    return (neg ? '-' : '') + bytes + ' B';
  }

  exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1000)), units.length - 1);
  // @ts-ignore
  bytes = (bytes / Math.pow(1000, exponent)).toFixed(2) * 1;
  unit = units[exponent];

  return (neg ? '-' : '') + bytes + ' ' + unit;
};

export const addHyphensToUuid = (uuid: string) => {
  return uuid?.replace(/([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{12})/, '$1-$2-$3-$4-$5') ?? '';
};

export const getPackArt = (packArt: any) => {
  if (typeof packArt === 'string') return packArt;
  let artP = packArt.filter((art: any) => art.type === 'square' || art.type === 'logo')[0];
  if (artP === undefined) {
    return null;
  }
  return artP.url;
};

/**
 * Creates a modpackch api url with the ability to inject an authentication key by default to the url
 *
 * @param url the remainder of the url (Automatically removes the `/` at the start of a string)
 */
export const createModpackchUrl = (url: string): string => {
  const key = store.state.auth?.token?.attributes.modpackschkey;
  return `${process.env.VUE_APP_MODPACK_API}/${key ?? 'public'}/${url.startsWith('/') ? url.slice(1) : url}`;
};

export type AbortableRequest = {
  abort: () => void;
  ready: Promise<Response>;
};

/**
 * A fetch that you can abort, automatically attaching a method that can be aborted
 *
 * @param request the normal fetch request args
 * @param opts    fetch request init
 */
export const abortableFetch = (request: RequestInfo, opts?: RequestInit): AbortableRequest => {
  const controller = new AbortController();
  const signal = controller.signal;

  return {
    abort: () => controller.abort(),
    ready: fetch(request, { ...opts, signal }),
  };
};

/**
 * Wraps the websocket send message in a timeout-able promise
 *
 * @deprecated use the typed version
 */
export const wsTimeoutWrapper = (payload: any, timeout: number = 10_000): Promise<any> => {
  return new Promise(async (resolve, reject) => {
    const timer = setTimeout(() => {
      reject('timed out');
    }, timeout);

    await store.dispatch('sendMessage', {
      payload,
      callback: (data: any) => {
        clearTimeout(timer);
        resolve(data);
      },
    });
  });
};

export const wsTimeoutWrapperTyped = <T, R>(payload: T, timeout: number = 10_000): Promise<R> => {
  return new Promise(async (resolve, reject) => {
    const timer = setTimeout(() => {
      reject('timed out');
    }, timeout);

    await store.dispatch('sendMessage', {
      payload,
      callback: (data: R) => {
        clearTimeout(timer);
        resolve(data);
      },
    });
  });
};
