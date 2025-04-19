import {SugaredInstanceJson} from '@/core/types/javaApi';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import {alertController} from '@/core/controllers/alertController';
import {createLogger} from '@/core/logger';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {InstanceController} from '@/core/controllers/InstanceController';
import { useInstallStore } from '@/store/installStore.ts';
import { useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useModpackStore } from '@/store/modpackStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';

export class InstanceActions {
  private static logger = createLogger("InstanceActions.ts");
  
  static async start(instance: SugaredInstanceJson) {
    if (!this.canStart(instance) || this.isUpdating(instance)) return false;
    
    await InstanceController.from(instance).play()
    return true;
  }
  
  static canStart(_: SugaredInstanceJson) {
    return true;
  }
  
  static async pin(instance: SugaredInstanceJson, pin: boolean) {
    const result = await sendMessage("pinInstance", {
      instance: instance.uuid,
      pin
    })
    
    if (result.success) {
      // Update the store
      const instancesStore = useInstanceStore();
      instancesStore.updateInstance(result.instance)
    }
    
    return result.success;
  }
  
  static isUpdating(instance: SugaredInstanceJson) {
    const installStore = useInstallStore();
    return installStore.currentInstall?.forInstanceUuid === instance.uuid;
  }
  
  static isRunning(instance: SugaredInstanceJson) {
    const runningInstancesStore = useRunningInstancesStore();
    return runningInstancesStore.instances.some(i => i.uuid === instance.uuid);
  }
  
  static async openSettings(instance: SugaredInstanceJson) {
    await safeNavigate(RouterNames.ROOT_LOCAL_PACK, {uuid: instance.uuid}, {quickNav: ModpackPageTabs.SETTINGS})
  }
  
  static async clearInstanceCache(announce = true) {
    InstanceActions.logger.debug("Clearing instance cache")
    
    const modpacksStore = useModpackStore();
    const instancesStore = useInstanceStore();
    
    await modpacksStore.clearModpacks();
    await instancesStore.loadInstances();
    
    if (announce) {
      alertController.success("Cache cleared");
    }
  }
}