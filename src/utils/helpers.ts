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
}

export function computeAspectRatio(width: number, height: number) {
  // Calculate the greatest common divisor of width and height using Euclid's algorithm
  const gcd: any = (a: number, b: number) => (b === 0 ? a : gcd(b, a % b));
  
  const aspectRatioGCD = gcd(width, height);

  // Simplify the aspect ratio
  const simplifiedWidth = width / aspectRatioGCD;
  const simplifiedHeight = height / aspectRatioGCD;
  
  return `${simplifiedWidth}:${simplifiedHeight}`;
}

export const parseMarkdown = (input: string) => markdownParser.render(input);

/**
 * Cursed method to flag valid but shouldn't be used console log statements to allow for easier 
 * removal later on
 */
export const consoleBadButNoLogger = (type: "L" | "I" | "D" | "E" | "W", ...args: any) => {
  switch (type) {
    case "I":
      console.info(...args);
      break;
    case "D":
      console.debug(...args);
      break;
    case "E":
      console.error(...args);
      break;
    case "W":
      console.warn(...args);
      break;
    case 'L':
    default:
      console.log(...args);
      break;
  }
}