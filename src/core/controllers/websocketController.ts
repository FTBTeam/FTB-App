import { Emitter } from 'mitt';
import { EmitEvents } from '@/bootstrap.ts';
import { createLogger } from '@/core/logger.ts';

export class WebsocketController {
  private websocket: WebSocket | null = null;
  private callbackQueue: { [key: string]: (data: any) => void } = {};
  
  constructor(
    private readonly emitter: Emitter<EmitEvents>,
    private readonly logger = createLogger("WebsocketController.ts")
  ) {
    this.maintainConnection().catch(console.error);
  }
  
  private async maintainConnection(source = "maintainConnection") {
    this.logger.info(`Attempting to connect to websocket server as part of ${source}`);
    try {
      const ws = await new Promise<WebSocket>((resolve, reject) => {
        const ws = new WebSocket("ws://localhost:13377");
        ws.onopen = () => {
          this.emitter.emit("ws/connected");
          resolve(ws);
        }
        
        ws.onerror = (e) => reject(e)
        ws.onclose = () => reject("Connection closed");
      });
      
      ws.onerror = (e) => console.error(e);
      
      ws.onmessage = (message) => {
        this.handleMessage(message);
      }

      // Try and connect again if the connection is closed
      ws.onclose = () => {
        console.log("Websocket closed");
        this.websocket = null;
        this.emitter.emit("ws/disconnected");
        setTimeout(() => this.maintainConnection("closed"), 5000);
      }
      
      this.websocket = ws;
    } catch (e) {
      console.error(e);
      // Timeout to prevent spamming the server
      setTimeout(() => this.maintainConnection("fatal"), 5000);
    }
  }
  
  private handleMessage(message: MessageEvent) {
    const rawMessage = message.data;
    if (!rawMessage.startsWith("{")) {
      this.logger.warn("Received invalid message", rawMessage);
      return;
    }
    
    const messageData = JSON.parse(rawMessage);
    this.emitter.emit("ws/message", messageData);
    
    if (this.callbackQueue[messageData.requestId]) {
      this.callbackQueue[messageData.requestId](messageData);
      delete this.callbackQueue[messageData.requestId];
    }
  }
  
  public send(requestId: string, request: {
    payload: any,
    callback: (data: any) => void
  }) {
    if (!this.websocket) {
      console.error("Websocket not connected, unable to send message");
      return;
    }
    
    // Attach the request ID to the payload
    request.payload.requestId = requestId;
    request.payload.secret = "no-implemented";
    
    this.websocket.send(JSON.stringify(request.payload));
    this.callbackQueue[requestId] = request.callback;
    return requestId;
  }
  
  public clearCallback(requestId: string) {
    if (!this.callbackQueue[requestId]) {
      return;
    }
    
    delete this.callbackQueue[requestId];
  }
  
  public isAlive() {
    return this.websocket !== null && this.websocket.readyState === WebSocket.OPEN;
  }
}