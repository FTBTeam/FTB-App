import {emitter} from '@/utils';
import store from '@/modules/store';
import {
  CloudSavesReloadedData,
  FilesEvent,
  InstallInstanceDataProgress,
  InstallInstanceDataReply, InstanceJson,
  SugaredInstanceJson
} from '@/core/@types/javaApi';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {PackProviders, Versions} from '@/modules/modpacks/types';

export type InstallRequest = {
  uuid: string;
  id: string | number;
  version: string | number;
  name: string;
  versionName: string;
  logo: string | null;
  updatingInstanceUuid?: string;
  category?: string;
  provider?: PackProviders;
  private: boolean
}

export type InstallStatus = {
  request: InstallRequest,
  status: "installing" | "failed" | "success";
  stage: string;
  progress: string;
  error?: string;
  forInstanceUuid: string;
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
    emitter.on("ws.message", async (data: any) => {
      if (data.type === "cloudInstancesReloaded") {
        this.addCloudInstances(data as CloudSavesReloadedData)
          .catch(err => console.error(err));
      }
    });
    
    // Force the queue to be checked on startup
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

  public async requestUpdate(instance: SugaredInstanceJson | InstanceJson, version: Versions, provider: PackProviders = "modpacksch") {
    this.queue.push({
      uuid: crypto.randomUUID(), // Not the same as the instance uuid
      id: instance.id,
      version: version.id,
      name: instance.name,
      versionName: version.name,
      logo: instance.art,
      updatingInstanceUuid: instance.uuid,
      category: instance.category,
      provider,
      private: instance._private,
    })
    
    // Trigger a check queue
    await this.checkQueue();
  }
  
  // TODO: Implement this
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
    const isUpdate = request.updatingInstanceUuid != null;

    
    // Make the installation request!
    const installResponse = await sendMessage("installInstance", {
      uuid: request.updatingInstanceUuid ?? "", // This flag is what tells the API to update an instance
      id: parseInt(request.id as string, 10),
      version: parseInt(request.version as string, 10),
      _private: request.private,
      packType: !request.provider ? 0 : (request.provider === "modpacksch" ? 0 : 1), // TODO: Support other providers
      shareCode: "", // TODO: Support share codes
      importFrom: null, // TODO: Support imports
      name: request.name,
      artPath: request.logo,
      category: request.category ?? "Default",
    });
    
    if (installResponse.status === "error" || installResponse.status === "prepare_error") {
      console.error("Failed to send install request", installResponse);
      this.installLock = false;
      return;
    }
    
    let knownInstanceUuid = installResponse.instanceData.uuid;
    if (installResponse.instanceData && !isUpdate) {
      // Don't add if it's an update otherwise we'll have two instances
      store.dispatch(`v2/instances/addInstance`, installResponse.instanceData, {root: true});
    }
    
    console.log("Install request sent", installResponse)
    
    const installRequest: InstallResult = await new Promise((resolve, reject) => {
      
      this.updateInstallStatus({
        request,
        status: "installing",
        progress: "0",
        stage: "Starting install",
        forInstanceUuid: knownInstanceUuid
      });
      
      const instanceInstaller = (data: InstallInstanceDataReply | InstallInstanceDataProgress | FilesEvent) => {
        if (data.type === "installInstanceDataReply") {
          const typedData = data as InstallInstanceDataReply;
          if (typedData.status === "error") {
            this.updateInstallStatus({
              request,
              status: "failed",
              stage: "Failed to install",
              progress: "0",
              error: typedData.message,
              forInstanceUuid: knownInstanceUuid
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
              forInstanceUuid: knownInstanceUuid
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
            forInstanceUuid: knownInstanceUuid
          })
        } else if (data.type === "install.filesEvent") {
          const typedData = data as FilesEvent;
          // NO OP for now
        }
      }
      
      const finish = (result: InstallResult) => {
        emitter.off("ws.message", instanceInstaller as any);
        this.updateInstallStatus(null)
        resolve(result);
      }
      
      emitter.on("ws.message", instanceInstaller  as any);
    });
    
    if (installRequest.success && installRequest.instance) {
      // Success! We always update as we've already added the instance to the store, this will toggle the installed state for the card.
      store.dispatch(`v2/instances/updateInstance`, installRequest.instance, {root: true});
      // TODO: Toast for success
    } else {
      // Failed!
      // TODO: Toast for error
      if (knownInstanceUuid) {
        store.dispatch(`v2/instances/removeInstance`, knownInstanceUuid, {root: true});
      }
    }
    
    this.installLock = false;
    await this.checkQueue(); // Force a queue check again 
  }
  
  private updateInstallStatus(status: InstallStatus | null) {
    store.commit("v2/install/SET_CURRENT_INSTALL", status);
  }
  
  private get queue() {
    return store.state["v2/install"].installQueue;
  }

  /**
   * Appends a cloud instance to the store if it doesn't already exist as these are loaded later in the lifecycle
   */
  private async addCloudInstances(payload: CloudSavesReloadedData) {
    console.log("Cloud instances reloaded", payload)
    if (!payload.changedInstances.length) {
      return;
    }
    
    for (const pack of payload.changedInstances) {
      // We shouldn't already have it but we might so keep it in sync regardless
      if ((store.state as any)['v2/instances'].instances.findIndex((i: InstanceJson) => i.uuid === pack.uuid) === -1) {
        console.log('Adding new instance', pack);
        await store.dispatch('v2/instances/addInstance', pack, {root: true});
      } else {
        console.log('Updating instance', pack);
        await store.dispatch('v2/instances/updateInstance', pack, {root: true});
      }
    }
  }
}

export const instanceInstallController = new InstanceInstallController();