import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {sendMessage} from '@/core/websockets/websocketsApi';
import store from '@/modules/store';
import {createLogger} from '@/core/logger';
import {LaunchingStatus} from '@/core/state/misc/runningState';
import {getProfileOrDefaultToActive} from '@/core/auth/authProfileSelector';
import {safeCheckProfileActive} from '@/core/auth/authValidChecker';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';

export type SaveJson = {
  name: string;
  jvmArgs: string;
  jrePath: string;
  programArgs: string;
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
  
  async play(profileUuid: string | null = null) {
    InstanceController.logger.debug(`Playing instance ${this.instance.uuid}`);
    
    InstanceController.logger.debug("Fetching active profile");
    const activeProfile = getProfileOrDefaultToActive(profileUuid);
    if (!activeProfile) {
      InstanceController.logger.warn("Failed to get active profile");
      return;
    }
    
    const loadingStatus: LaunchingStatus = {
      uuid: this.instance.uuid,
      loggingIn: true,
      starting: false,
      error: "",
      step: "Checking profile",
    }
    
    await store.dispatch('v2/running/updateLaunchingStatus', loadingStatus);

    InstanceController.logger.debug("Checking profile");
    const checkResult = await safeCheckProfileActive(activeProfile.uuid);
    if (checkResult !== "VALID") {
      InstanceController.logger.warn("Failed to check profile");
      
      if (checkResult === "TOTAL_FAILURE") {
        loadingStatus.loggingIn = false;
        loadingStatus.error = "Failed to check profile";
        await store.dispatch('v2/running/updateLaunchingStatus', loadingStatus);
        return;
      }
      
      if (checkResult === "NOT_LOGGED_IN") {
        InstanceController.logger.debug("Profile is not logged in, asking the user to sign in");
        // Get the user to log back in again
        await store.dispatch('v2/running/clearLaunchingStatus');
        await store.dispatch('core/openSignIn')
      }
      
      return;
    }
    
    loadingStatus.loggingIn = false;
    loadingStatus.starting = true;
    loadingStatus.step = "Starting instance";
    await store.dispatch('v2/running/updateLaunchingStatus', loadingStatus);

    InstanceController.logger.debug("Sending launch handshake");
    const result = await sendMessage("launchInstance", {
      uuid: this.instance.uuid,
      extraArgs: "",
      offline: false,
      offlineUsername: "",
      cancelLaunch: null
    }, 1000 * 60 * 10) // 10 minutes It's a long time, but we don't want to timeout too early

    if (result.status === 'success') {
      InstanceController.logger.debug("Successfully completed launch handshake")
    } else {
      if (result.status === 'error') {
        loadingStatus.error = result.message;  
      } else {
        loadingStatus.error = "Failed to launch instance";
      }
      
      InstanceController.logger.warn("Failed to launch instance", result);
      loadingStatus.starting = false;
      await store.dispatch('v2/running/updateLaunchingStatus', loadingStatus);
      return;
    }

    await store.dispatch('v2/running/clearLaunchingStatus');
    
    InstanceController.logger.debug("Navigating to running instance");
    await safeNavigate(RouterNames.ROOT_RUNNING_INSTANCE, {uuid: this.instance.uuid});
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