// import MarkdownIt from 'markdown-it';
import Router, {RouterNames} from '@/router';

// TODO: [port] fixme
const markdownParser = {} as any;

// const markdownParser = new MarkdownIt();
// markdownParser.renderer.rules.link_open = function (tokens, idx, options, env, self) {
//   tokens[idx].attrSet('onclick', 'event.preventDefault(); window.platform.get.utils.openUrl(this.href);');
//   return self.renderToken(tokens, idx, options);
// }

export async function safeNavigate(name: RouterNames, params?: any, query?: any) {
  if (Router.currentRoute.name === name) {
    return;
  }
  
  try {
    await Router.push({name, params, query});
  } catch (e) {
    // Ignore
  }
}

// Sizes of various byte amounts
export const kilobyteSize: number = 1024;
export const megabyteSize: number = 1024 * kilobyteSize;
export const gigabyteSize: number = 1024 * megabyteSize;

export const prettyByteFormat = (bytes: number) => {
  if (bytes === 0) return '0B';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const formattedValue = parseFloat((bytes / Math.pow(k, i)).toFixed(2));

  return `${formattedValue.toFixed(2)} ${sizes[i]}`;
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