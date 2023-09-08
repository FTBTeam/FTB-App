/**
 * Gobbles an error and returns a default value, good for when we don't care about the error
 * but we're likely expecting one.
 */
export async function gobbleError<T>(call: () => T, defaultValue?: T) {
  try {
    return call()
  } catch (e) {
    return defaultValue ?? null;
    // Ignored
  }
}

/**
 * Runs a method before and after a promise resolves
 */
export async function runBeforeAndAfter<T>(before: () => void, after: () => void, promise: () => Promise<T>) {
  before();
  try {
    return await promise();
  } catch (e) {
    throw e;
  } finally {
    after();
  }
}

/**
 * Toggle a boolean value before and after a promise resolves
 */
export async function toggleBeforeAndAfter<T>(promise: () => Promise<T>, onStageChange: (state: boolean) => void): Promise<T> {
  return runBeforeAndAfter(() => onStageChange(true), () => onStageChange(false), promise);
}