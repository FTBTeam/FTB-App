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
  if (bytes === 0) return '0B';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const formattedValue = parseFloat((bytes / Math.pow(k, i)).toFixed(2));

  return `${formattedValue}${sizes[i]}`;
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