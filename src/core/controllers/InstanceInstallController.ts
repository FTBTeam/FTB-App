import {emitter} from '@/utils';
import store from '@/modules/store';
import {
  FilesEvent,
  InstallInstanceDataProgress,
  InstallInstanceDataReply,
  SugaredInstanceJson
} from '@/core/@types/javaApi';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Versions} from '@/modules/modpacks/types';

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
  stage: string;
  progress: string;
  error?: string;
}

type InstallResult = {
  success: boolean;
  instance?: SugaredInstanceJson;
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

  public async requestUpdate(instance: SugaredInstanceJson, version: Versions) {
    this.queue.push({
      uuid: crypto.randomUUID(),
      id: instance.id,
      version: version.id,
      name: instance.name,
      versionName: version.name,
      logo: instance.art,
      updatingInstanceUuid: instance.uuid,
    })
    
    // Trigger a check queue
    await this.checkQueue();
  }
  
  public async requestImport() {
    
  }
  
  private async checkQueue() {
    if (this.queue.length === 0 || this.installLock) {
      return;
    }
    
    const request = await store.dispatch("v2/install/popInstallQueue", {root: true}) as InstallRequest | null;
    if (request == null) {
      return;
    }
    
    // Don't holt the queue while we install
    this.installPack(request)
      .catch(err => {
        console.error(err);
      })
  }

  /**
   *  An install goes through a few stages and thus each stage is supported below,
   *  The basic outline of these stages is:
   *  
   *  1. Prepare, at this point, we're still trying to resolve the modapck back from the API
   *    a. It's possible for the prepare to fail and we have to hand that. We don't know anything 
   *    about the pack at this point, so we can't really do anything other than display an error.
   *    b. An instance might already be installing and if it is, we should push the install request 
   *    back onto the queue and wait for the current install to finish.
   *  2. Init, at this point we know the instance id and the install should start
   *  3. Installing, at this point we're waiting for the install to complete
   *  4. Success, the install completed successfully
   */
  private async installPack(request: InstallRequest) {
    if (this.installLock) {
      console.log("Locking install queue")
      return;
    }
    
    this.installLock = true;

    // Make the install request!
    const installResponse = await sendMessage("installInstance", {
      uuid: request.updatingInstanceUuid ?? "",
      id: parseInt(request.id as string, 10),
      version: parseInt(request.version as string, 10),
      _private: false,
      packType: 0,
      shareCode: "",
      importFrom: null,
      name: request.name,
    });
    
    if (installResponse.status === "error" || installResponse.status === "prepare_error") {
      console.error("Failed to send install request", installResponse);
      this.installLock = false;
      return;
    }
    
    console.log("Install request sent", installResponse)
    
    const installRequest: InstallResult = await new Promise((resolve, reject) => {
      
      this.updateInstallStatus({
        request,
        status: "installing",
        progress: "0",
        stage: "Starting install"
      });
      
      const instanceInstaller = (data: InstallInstanceDataReply | InstallInstanceDataProgress | FilesEvent) => {
        if (data.type === "installInstanceDataReply") {
          // TODO: If the status is init, we should update the instance store to reflect the instance being installed
          //       but we should ensure you can't do anything with it until it's done.
          const typedData = data as InstallInstanceDataReply;
          if (typedData.status === "error") {
            this.updateInstallStatus({
              request,
              status: "failed",
              stage: "Failed to install",
              progress: "0",
              error: typedData.message
            });
            
            return finish({
              success: false,
            });
          } else if (typedData.status === "success") {
            this.updateInstallStatus({
              request,
              stage: "Installed",
              status: "success",
              progress: "100",
            });
            
            return finish({
              success: true,
              instance: typedData.instanceData as SugaredInstanceJson, // It's not really but it's fine
            });
          } else if (typedData.status === "files") {
            // No Op for now...
            // TODO: Figure this one out.
          }
        } else if (data.type === "installInstanceProgress") {
          const typedData = data as InstallInstanceDataProgress;
          this.updateInstallStatus({
            request,
            stage: toTitleCase(typedData.currentStage),
            status: "installing",
            progress: typedData.overallPercentage.toFixed(1),
          })
        } else if (data.type === "install.filesEvent") {
          const typedData = data as FilesEvent;
          // NO OP for now
        }
      }
      
      const finish = (result: InstallResult) => {
        emitter.off("ws.message", instanceInstaller as any);
        // TODO: Do something with an error
        // TODO: If failed, remove the instance from the store as it's not installed
        // TODO: If successful, update the instance store correctly
        this.updateInstallStatus(null)
        resolve(result);
      }
      
      emitter.on("ws.message", instanceInstaller  as any);
    });
    
    if (installRequest.success) {
      // Success!
      store.dispatch(`v2/instances/${request.updatingInstanceUuid ? 'updateInstance' : 'addInstance'}`, installRequest.instance, {root: true});
    } else {
      // Failed!
      // Do something with the error
    }
    
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