/**
 * Converts a string to title case
 */
export const toTitleCase = (str: string) => {
  return str.replace(/\w\S*/g, (txt) => {
    return txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase();
  });
}

/**
 * Converts a string to sentence case
 */
export const toSentenceCase = (str: string) => {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Creates a localised number string
 */
export const localiseNumber = (num: number) => {
  if (!num) return '0';
  
  return num.toLocaleString();
}

/**
 * String equals ignoring case
 */
export const equalsIgnoreCase = (a: string, b: string) => {
  return a.toLowerCase() === b.toLowerCase();
}

/**
 * String contains ignoring case
 */
export const containsIgnoreCase = (a: string, b: string) => {
  return a.toLowerCase().includes(b.toLowerCase());
}

/**
 * Removes any trailing slashes from a uri
 * 
 * @param uri The uri to remove the trailing slash from
 */
export function removeTailingSlash(uri: string): string {
  if (uri.endsWith('/')) {
    return uri.slice(0, -1);
  }

  return uri;
}

/**
 * Creates a clean looking, shortened number format.
 * Aka: 1000 -> 1k, 1000000 -> 1m, 1000000000 -> 1b
 */
export function prettyNumber(num: number): string {
  return Intl.NumberFormat("en", {notation: "compact"}).format(num);
}

/**
 * Create trimmed string
 */
export function trimString(str: string, maxLength: number): string {
  if (str.length > maxLength) {
    return str.substring(0, maxLength) + '...';
  }

  return str;
}

export function stringIsEmpty(str: string | undefined | null) {
  return !str || str.trim() === '';
}

export function stringOrDefault(str: string | undefined | null, defaultStr: string) {
  return stringIsEmpty(str) ? defaultStr : str;
}