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