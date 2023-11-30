import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import store from '@/modules/store';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import {alertController} from '@/core/controllers/alertController';

export class InstanceActions {
  static async start(instance: SugaredInstanceJson) {
    if (!this.canStart(instance) || this.isUpdating(instance)) return false;
    
    await safeNavigate(RouterNames.ROOT_LAUNCH_PACK, undefined, {uuid: instance.uuid})
    return true;
  }
  
  static canStart(instance: SugaredInstanceJson) {
    return !instance.pendingCloudInstance;
  }
  
  static isUpdating(instance: SugaredInstanceJson) {
    return store.state["v2/install"].currentInstall?.forInstanceUuid === instance.uuid;
  }
  
  static async openSettings(instance: SugaredInstanceJson) {
    await safeNavigate(RouterNames.ROOT_LOCAL_PACK, {uuid: instance.uuid}, {quickNav: ModpackPageTabs.SETTINGS})
  }
  
  static clearInstanceCache(announce = true) {
    store.dispatch("v2/modpacks/clearModpacks", undefined, {
      root: true
    });
    
    store.dispatch("v2/instances/loadInstances", undefined, {
      root: true
    }); // Reload instances
    
    if (announce) {
      alertController.success("Cache cleared");
    }
  }
}