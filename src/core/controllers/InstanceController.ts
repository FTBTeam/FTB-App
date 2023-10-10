import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {sendMessage} from '@/core/websockets/websocketsApi';
import store from '@/modules/store';

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
  category: string;
}

/**
 * Wrapper controller for an instance to ensure consistency in the store 
 */
export class InstanceController {
  private constructor(private readonly instance: SugaredInstanceJson | InstanceJson) {
    if (this.instance === null || this.instance === undefined) {
      throw new Error('Instance cannot be null or undefined');
    }
  }
  
  static from(instance: SugaredInstanceJson | InstanceJson) {
    return new InstanceController(instance);
  }
  
  async updateInstance(data: SaveJson) {
    const result = await sendMessage("instanceConfigure", {
      uuid: this.instance.uuid,
      instanceJson: JSON.stringify(data)
    })

    if (result.status === "success") {
      // Update the store
      await store.dispatch('v2/instances/updateInstance', result.instanceJson);
      return true;
    }
    
    return false;
  }
  
  async deleteInstance() {
    const result = await sendMessage('uninstallInstance', {
      uuid: this.instance.uuid
    });

    if (result.status === 'success') {
      // Remove it from the store
      await store.dispatch('v2/instances/removeInstance', this.instance.uuid);
      return true;
    }
    
    return false;
  }
}