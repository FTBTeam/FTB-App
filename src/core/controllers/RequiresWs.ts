export abstract class RequiresWs {
  abstract onConnected(): void;
  
  onDisconnected() {}
}