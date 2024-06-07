import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {sendMessage} from '@/core/websockets/websocketsApi';
import store from '@/modules/store';
import {createLogger} from '@/core/logger';

export type SaveJson = {
  name: string;
  jvmArgs: string;
  jrePath: string;
  memory: number;
  width: number;
  height: number;
  cloudSaves: boolean;
  fullScreen: boolean;
  releaseChannel: string;
  instanceImage?: string;
  preventMetaModInjection?: boolean;
  category: string;
  locked: boolean;
  shellArgs: string;
}

/**
 * Wrapper controller for an instance to ensure consistency in the store 
 */
export class InstanceController {
  private static logger = createLogger("InstanceController.ts");
  
  private constructor(private readonly instance: SugaredInstanceJson | InstanceJson) {
    if (this.instance === null || this.instance === undefined) {
      throw new Error('Instance cannot be null or undefined');
    }
  }
  
  static from(instance: SugaredInstanceJson | InstanceJson) {
    return new InstanceController(instance);
  }
  
  async updateInstance(data: SaveJson) {
    InstanceController.logger.debug("Updating instance", data);
    const result = await sendMessage("instanceConfigure", {
      uuid: this.instance.uuid,
      instanceJson: JSON.stringify(data)
    })

    if (result.status === "success") {
      // Update the store
      await store.dispatch('v2/instances/updateInstance', result.instanceJson);
      return result;
    }

    InstanceController.logger.warn("Failed to update instance", result);
    return null
  }
  
  async deleteInstance() {
    InstanceController.logger.debug(`Deleting instance ${this.instance.uuid}`);
    const result = await sendMessage('uninstallInstance', {
      uuid: this.instance.uuid
    });

    if (result.status === 'success') {
      // Remove it from the store
      await store.dispatch('v2/instances/removeInstance', this.instance.uuid);
      return true;
    }
    
    InstanceController.logger.warn("Failed to delete instance", result);
    return false;
  }
}