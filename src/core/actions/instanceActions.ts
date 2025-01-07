import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import store from '@/modules/store';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import {alertController} from '@/core/controllers/alertController';
import {createLogger} from '@/core/logger';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {InstanceController} from '@/core/controllers/InstanceController';
import {RunningState} from '@/core/state/misc/runningState';

export class InstanceActions {
  private static logger = createLogger("InstanceActions.ts");
  
  static async start(instance: SugaredInstanceJson) {
    if (!this.canStart(instance) || this.isUpdating(instance)) return false;
    
    await InstanceController.from(instance).play()
    // await safeNavigate(RouterNames.ROOT_LAUNCH_PACK, undefined, {uuid: instance.uuid})
    return true;
  }
  
  static canStart(instance: SugaredInstanceJson) {
    return true;
  }
  
  static async pin(instance: SugaredInstanceJson, pin: boolean) {
    const result = await sendMessage("pinInstance", {
      instance: instance.uuid,
      pin
    })
    
    if (result.success) {
      // Update the store
      await store.dispatch(`v2/instances/updateInstance`, result.instance, {root: true});
    }
    
    return result.success;
  }
  
  static isUpdating(instance: SugaredInstanceJson) {
    return store.state["v2/install"].currentInstall?.forInstanceUuid === instance.uuid;
  }
  
  static isRunning(instance: SugaredInstanceJson) {
    return (store.state["v2/running"] as RunningState).instances.some(i => i.uuid === instance.uuid);
  }
  
  static async openSettings(instance: SugaredInstanceJson) {
    await safeNavigate(RouterNames.ROOT_LOCAL_PACK, {uuid: instance.uuid}, {quickNav: ModpackPageTabs.SETTINGS})
  }
  
  static async clearInstanceCache(announce = true) {
    InstanceActions.logger.debug("Clearing instance cache")
    await store.dispatch("v2/modpacks/clearModpacks", undefined, {
      root: true
    });
    
    await store.dispatch("v2/instances/loadInstances", undefined, {
      root: true
    }); // Reload instances
    
    if (announce) {
      alertController.success("Cache cleared");
    }
  }
}