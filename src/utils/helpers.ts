import MarkdownIt from 'markdown-it';
import Router, {RouterNames} from '@/router';

const markdownParser = new MarkdownIt();
markdownParser.renderer.rules.link_open = function (tokens, idx, options, env, self) {
  tokens[idx].attrSet('onclick', 'event.preventDefault(); window.platform.get.utils.openUrl(this.href);');
  return self.renderToken(tokens, idx, options);
}

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