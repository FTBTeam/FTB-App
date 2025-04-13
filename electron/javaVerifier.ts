import { ipcMain } from 'electron';

export class JavaVerifier {
  async verifyJava() {
    
  }
  
  private emitUpdate(message: string) {
    ipcMain.emit("java/message", message)
  }
}