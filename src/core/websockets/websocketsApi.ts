import {MessagePayload} from '@/core/websockets/websocketsEndpoints';
import store from '@/modules/store';
import {BaseData} from '@/core/@types/javaApi';
import {ApiEndpoints} from '@/core/@types/javaApiEndpoints';

/**
 * Sends a message to the backend and returns the response.
 * 
 * @description
 *   If T errors and says it can not be used as an index type, it means the
 *   MessagePayload type does not have a key of MessageType.
 */
export function sendMessage<T extends ApiEndpoints>(
  messageType: T, 
  payload?: MessageRaw<MessagePayload[T]["input"]>, 
  timeout = 30_000
): Promise<MessagePayload[T]["output"] & { messageId: string }> {
  return new Promise(async (resolve, reject) => {
    const timer = setTimeout(() => {
      reject(`Failed to resolve response from [type: ${messageType}]`);
    }, timeout);

    // This should be the only type this dispatch is ever used
    const messageId = await store.dispatch('sendMessage', {
      payload: {
        type: messageType,
        ...payload,
      },
      callback: (data: MessagePayload[T]["output"]) => {
        clearTimeout(timer);
        resolve({...data, messageId: messageId ?? "unknown"});
      },
    });
  });
}

export type MessageRaw<T extends BaseData> = Omit<T, "type" | "requestId" | "secret">;

// Helper type to allow specific fields to be set as their original type or null.
// This fixes a shortfall of the typescript generation system and Nullable retention 
export type Nullable<T, K extends keyof T> = {
  [P in keyof T]: P extends K ? T[P] | null : T[P];
}