import {emitter} from '@/utils';
import store from '@/modules/store';

export type InstallRequest = {
  uuid: string;
  id: string | number;
  version: string | number;
  name: string;
  versionName: string;
  logo: string;
  updatingInstanceUuid?: string;
}

export type InstallStatus = {
  request: InstallRequest,
  status: "installing" | "failed" | "success";
  progress: number;
  error?: string;
}

/**
 * Instance install controller backed by a vuex store queue
 */
class InstanceInstallController {
  private installLock = false;
  
  constructor() {
    this.checkQueue()
      .catch(err => console.error(err));
    
    setInterval(() => {
      this.checkQueue()
        .catch(err => console.error(err));
    }, 5000); // 5 seconds
  }
  
  public async requestInstall(request: Omit<InstallRequest, "uuid">) {
    this.queue.push({
      uuid: crypto.randomUUID(),
      ...request,
    });
    
    // Trigger a check queue
    await this.checkQueue();
  }
  
  private async checkQueue() {
    console.log("Checking queue", this.queue.length, this.installLock)
    
    if (this.queue.length === 0 || this.installLock) {
      return;
    }
    
    const request = await store.dispatch("v2/install/popInstallQueue", {root: true}) as InstallRequest | null;
    console.log(request);
    if (request == null) {
      return;
    }
    
    // Don't holt the queue while we install
    this.installPack(request)
      .catch(err => {
        console.error(err);
      })
  }
  
  private async installPack(request: InstallRequest) {
    this.installLock = true;
    
    const installRequest = await new Promise((resolve, reject) => {
      this.updateInstallStatus({
        request,
        status: "installing",
        progress: 0,
      });
      
      const instanceInstaller = (data: any) => {
        finish(true);
      }
      
      const finish = (success: boolean) => {
        emitter.off("ws.message", instanceInstaller);
        this.updateInstallStatus(null)
        resolve(success);
      }
      
      emitter.on("ws.message", instanceInstaller);
      setTimeout(() => {
        finish(true);
      }, 5000);
    });
    
    console.log(installRequest)
    
    this.installLock = false;
    await this.checkQueue(); // Force a queue check again 
  }
  
  private updateInstallStatus(status: InstallStatus | null) {
    store.commit("v2/install/SET_CURRENT_INSTALL", status);
  }
  
  private get queue() {
    return store.state.v2["v2/install"].installQueue;
  }
}

export const instanceInstallController = new InstanceInstallController();