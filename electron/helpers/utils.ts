import log from "electron-log/main";

export function logAndReturn<T>(value: T, message: string): T {
  log.debug(message, value);
  return value;
}

export async function logAndReturnAsync<T>(value: T, message: string): Promise<T> {
  log.debug(message, value);
  return value;
}