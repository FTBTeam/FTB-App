import {createLogger} from '@/core/logger';
import { useAppStore } from '@/store/appStore.ts';

export type Alert = {
  type: "success" | "error" | "warning" | "info";
  message: string;
  persistent?: boolean;
  onClose?: () => void;
}

type AlertOptions = {
  type: "success" | "error" | "warning" | "info"
  persistent?: boolean;
  onClose?: () => void;
}

class AlertController {
  private logger = createLogger("AlertController.ts");
  
  success(message: string, options: Omit<AlertOptions, "type"> = {}) {
    this.createAlert(message, { type: "success", ...options })
  }
  
  error(message: string, options: Omit<AlertOptions, "type"> = {}) {
    this.createAlert(message, { type: "error", ...options, persistent: true })
    this.logger.error("[alert] " + message)
  }
  
  warning(message: string, options: Omit<AlertOptions, "type"> = {}) {
    this.createAlert(message, { type: "warning", ...options })
    this.logger.warn("[alert] " + message)
  }
  
  info(message: string, options: Omit<AlertOptions, "type"> = {}) {
    this.createAlert(message, { type: "info", ...options })
  }
  
  createAlert(message: string, options: AlertOptions) {
    const appStore = useAppStore();
    
    appStore.emitter.emit("alert/simple", {message, ...options} as Alert)
  }
}

export const alertController = new AlertController(); 