/**
 * Takes an array and splits it into chunks of a specified size
 * 
 * @param array The array to split
 * @param size The size of each chunk
 */
export function chunkArray<T>(array: T[], size: number): T[][] {
  const results = [];
  while (array.length) {
    results.push(array.splice(0, size));
  }
  return results;
}

/**
 * Shuffles an array using the Fisher-Yates algorithm
 */
function shuffle<T>(arr: T[]): T[] {
  for (let i = arr.length -1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    const temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
  }
  return arr;
}

/**
 * Array sorter taking in T[] and returning T[], accepting a field finder method and a sort direction
 */
export function sortBy<T>(array: T[], fieldFinder: (item: T) => any, direction: 'asc' | 'desc' = 'asc'): T[] {
  return array.sort((a, b) => {
    const aField = fieldFinder(a);
    const bField = fieldFinder(b);

    if (aField < bField) {
      return direction === 'asc' ? -1 : 1;
    }
    if (aField > bField) {
      return direction === 'asc' ? 1 : -1;
    }
    return 0;
  });
}