import {MessagePayload, MessageType} from '@/core/websockets/websocketsEndpoints';
import store from '@/modules/store';

/**
 * Sends a message to the backend and returns the response.
 * 
 * @description
 *   If T errors and says it can not be used as an index type, it means the
 *   MessagePayload type does not have a key of MessageType.
 */
export function sendMessage<T extends MessageType>(
  messageType: T, 
  payload: MessagePayload[T]["input"], 
  timeout = 30_000
): Promise<MessagePayload[T]["output"]> {
  return new Promise(async (resolve, reject) => {
    const timer = setTimeout(() => {
      reject(`Failed to resolve response from [type: ${messageType}]`);
    }, timeout);

    await store.dispatch('sendMessage', {
      payload: {
        type: messageType,
        ...payload,
      },
      callback: (data: MessagePayload[T]["output"]) => {
        clearTimeout(timer);
        resolve(data);
      },
    });
  });
}