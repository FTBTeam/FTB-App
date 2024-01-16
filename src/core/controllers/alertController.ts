import {emitter} from '@/utils';
import {createLogger} from '@/core/logger';

export type Alert = {
  type: "success" | "error" | "warning" | "info";
  message: string;
}

class AlertController {
  private logger = createLogger(AlertController.name + ".ts");
  
  success(message: string) {
    this.createAlert(message, "success")
  }
  
  error(message: string) {
    this.createAlert(message, "error")
    this.logger.error("[alert] " + message)
  }
  
  warning(message: string) {
    this.createAlert(message, "warning")
    this.logger.warn("[alert] " + message)
  }
  
  info(message: string) {
    this.createAlert(message, "info")
  }
  
  createAlert(message: string, type: "success" | "error" | "warning" | "info") {
    (emitter).emit("alert.simple", {message, type} as Alert)
  }
}

export const alertController = new AlertController(); 