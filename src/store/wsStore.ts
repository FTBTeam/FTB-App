import { defineStore } from 'pinia';
import { useAppStore } from '@/store/appStore.ts';
import { useModalStore } from '@/store/modalStore.ts';

type WsStore = {
  ready: boolean;
  wsSecret: string | null;
  controller: WebSocketController;
}

export const useWsStore = defineStore("ws", {
  state: (): WsStore => {
    const wsController = new WebSocketController(
      (ws: WebSocket) => {
        console.log("Websocket connected", ws);
        const appStore = useWsStore();
        appStore.ready = true;
      },
      () => {
        console.log("Websocket disconnected");

        const appStore = useWsStore();
        appStore.ready = false;
      }
    );
    
    return {
      ready: false,
      wsSecret: null,
      controller: wsController,
    }
  },

  actions: {
    sendMessage(requestId: string, request: {
      payload: any,
      callback: (data: any) => void
    }) {
      // Attach the request ID to the payload
      request.payload.requestId = requestId;
      request.payload.secret = this.wsSecret;

      this.controller.send(JSON.stringify(request.payload));
      if (request.callback) {
        this.controller.addCallback(requestId, request.callback);
      }
      
      return requestId;
    },
    clearCallback(requestId: string) {
      this.controller.removeCallback(requestId);
    }
  }
})

/**
 * Simple websocket controller to ensure we always have a connection
 * and to handle sending and receiving messages.
 */
class WebSocketController {
  private ws: WebSocket | null = null;
  private callbackQueue: { [key: string]: (data: any) => void } = {};
  private dataQueue: any[] = [];
  
  private connectingLock = false;

  /**
   * Custom hooks to pass back to the main store to avoid non-reactive
   * data being stored in this class
   */
  constructor(
    private readonly onConnect: (ws: WebSocket) => void,
    private readonly onDisconnect: () => void,
  ) {
    this.setup();
  }
  
  private setup() {
    this.connect()
    this.callbackQueue = {};
  }
  
  private connect(attempts = 0) {
    if (this.connectingLock) {
      return;
    }
    
    if (this.ws) {
      this.ws.close();
    }
    
    this.connectingLock = true;
    try {
      this.ws = new WebSocket("ws://localhost:13377");
      this.attachEvents(this.ws);
    } catch (e) {
      console.error(e);
      setTimeout(() => this.connect(attempts ++), 5000);
    } finally {
      this.connectingLock = false;
    }
  }
  
  private reconnect(reason = "unknown") {
    console.log("Reconnecting to websocket", reason);
    
    this.onDisconnect();
    if (this.ws) {
      if (this.ws.readyState === WebSocket.OPEN) {
        this.ws.close();
      }
      
      this.ws = null;
    }
    
    setTimeout(() => this.connect(), 5000);
  }
  
  private attachEvents(ws: WebSocket) {
    ws.onerror = () => {
      console.warn("Websocket error, reconnecting");
    }
    
    ws.onclose = (event) => {
      this.reconnect(`close: ${event.code} ${event.reason}`);
    }
    
    ws.onmessage = (message) => {
      this.onMessage(message)
    }
    
    ws.onopen = async () => {
      await this.waitForReady(ws);
      this.onConnect(ws);
      this.processQueue()
    }
  }
  
  private onMessage(message: MessageEvent) {
    const appStore = useAppStore();
    
    const rawMessage = message.data;
    if (!rawMessage.startsWith("{")) {
      console.warn("Received invalid message", rawMessage);
      return;
    }

    const messageData = JSON.parse(rawMessage);
    const handled = this.handleSpecialMessages(messageData);
    if (handled) {
      return;
    }
    
    if (!messageData?.notViableForLogging) {
      console.debug(`Recieved message: [${messageData.requestId ?? 'unknown'}::${messageData.type}]`, messageData);
    }
    
    appStore.emitter.emit("ws/message", messageData);
    
    this.triggerCallback(messageData.requestId, messageData);
  }
  
  public send(data: any) {
    // Add the message to the queue
    this.dataQueue = [...this.dataQueue, data];
    this.processQueue();
  }
  
  private processQueue() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }

    while (this.dataQueue.length > 0) {
      const data = this.dataQueue.shift();
      if (data) {
        this.ws.send(data);
      }
    }
  }
  
  public addCallback(requestId: string, callback: (data: any) => void) {
    if (this.callbackQueue[requestId]) {
      console.warn(`Callback for requestId ${requestId} already exists`);
      return;
    }

    this.callbackQueue[requestId] = callback;
  }
  
  public triggerCallback(requestId: string, data: any) {
    if (this.callbackQueue[requestId]) {
      this.callbackQueue[requestId](data);
      delete this.callbackQueue[requestId];
    }
  }
  
  public removeCallback(requestId: string) {
    if (!this.callbackQueue[requestId]) {
      return;
    }

    delete this.callbackQueue[requestId];
  }
  
  private waitForReady(ws: WebSocket) {
    return new Promise<void>((resolve) => {
      const interval = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          clearInterval(interval);
          resolve();
        }
      }, 100);
    });
  }
  
  private handleSpecialMessages(message: any) {
    if (message.type === "ping") {
      // Respond to ping messages
      // TODO: More data?
      this.ws?.send(JSON.stringify({ type: "pong" }));
      return true;
    }
    
    if (message.type === "openModal" || message.type === "closeModal") {
      const modalStore = useModalStore();
      if (message.type === "openModal") {
        modalStore.openModal(message);
      } else {
        modalStore.closeModal();
      }
      
      return true;
    }
    
    return false;
  }
}