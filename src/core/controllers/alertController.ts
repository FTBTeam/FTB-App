import {emitter} from '@/utils';

export type Alert = {
  type: "success" | "error" | "warning" | "info";
  message: string;
}

class AlertController {
  success(message: string) {
    this.createAlert(message, "success")
  }
  
  error(message: string) {
    this.createAlert(message, "error")
  }
  
  warning(message: string) {
    this.createAlert(message, "warning")
  }
  
  info(message: string) {
    this.createAlert(message, "info")
  }
  
  createAlert(message: string, type: "success" | "error" | "warning" | "info") {
    (emitter).emit("alert.simple", {message, type} as Alert)
  }
}

export const alertController = new AlertController(); 