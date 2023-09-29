import store from '@/modules/store';
import MarkdownIt from 'markdown-it';
import Router, {RouterNames} from '@/router';

const markdownParser = new MarkdownIt();

export async function safeNavigate(name: RouterNames, params?: any, query?: any) {
  if (Router.currentRoute.name === name) {
    return;
  }
  
  try {
    Router.push({name, params, query});
  } catch (e) {
    // Ignore
  }
}

// TODO: Rewrite, this isn't a great implementation.
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

/**
 * Creates a modpackch api url with the ability to inject an authentication key by default to the url
 *
 * @param url the remainder of the url (Automatically removes the `/` at the start of a string)
 * @param forcePublic forces the request to be public
 * 
 * @deprecated use the new version instead.
 */
export const createModpackchUrl = (url: string, forcePublic = false): string => {
  const key = store.state.auth?.token?.attributes.modpackschkey;
  return `${process.env.VUE_APP_MODPACK_API}/${key && !forcePublic ? key : 'public'}/${
    url.startsWith('/') ? url.slice(1) : url
  }`;
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

export const parseMarkdown = (input: string) => markdownParser.render(input); 