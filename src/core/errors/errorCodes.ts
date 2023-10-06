/**
 * Codes follow a simple schema, your prefix (always ftb start), the system
 * it relates to, then finally a code starting in the one thousand range
 * iterating up for each new error type. Do not attempt to clean up dead codes,
 * if an error is no longer used, simply remove it, do not change the iteration order.
 */
const Codes: Readonly<Record<string, string>> = {
  'ftb-errors#1000': 'Unable to find any valid error code...',
  'ftb-auth#1000': 'No profiles found, at least one is required',
  'ftb-auth#1001': 'No active profiles found, an active profile is required',
  'ftb-auth#1002': 'Attempted to refresh user profile but failed',
  'ftb-auth#1003': 'Legacy Mojang accounts are no longer supported',
};

export type ErrorCodes = keyof typeof Codes;

export type ErrorCode = {
  message: string;
  code: ErrorCodes;
};

/**
 * Creates an error code based on the provided error code by ensuring the error code
 * is valid preventing the possibility of unknown error codes.
 *
 * @param code the code to build
 */
export const createError = (code: ErrorCodes): ErrorCode => {
  // This isn't really possible due to the type contracts here...
  if (!Codes[code]) {
    console.log(`[error] Failed to find error code {${code}`);

    if (code !== 'ftb-errors#1000') {
      return createError('ftb-errors#1000');
    }

    throw new Error('Unable to create error code due to ftb-errors#1000 not existing somehow?');
  }

  return {
    code,
    message: Codes[code],
  };
};
