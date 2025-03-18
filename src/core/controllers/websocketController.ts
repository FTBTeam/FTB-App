import { Emitter } from 'mitt';
import { EmitEvents } from '@/bootstrap.ts';
import { createLogger } from '@/core/logger.ts';

export class WebsocketController {
  private websocket: WebSocket | null = null;
  
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
    console.log(message);
    // this.emitter.emit("ws/message", message.data);
  }
  
  public send(json: object) {
    if (!this.websocket) {
      console.error("Websocket not connected, unable to send message");
      return;
    }
    
    this.websocket.send(JSON.stringify(json));
  }
  
  public isAlive() {
    return this.websocket !== null && this.websocket.readyState === WebSocket.OPEN;
  }
}