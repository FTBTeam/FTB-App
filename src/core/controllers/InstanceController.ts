import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {createLogger} from '@/core/logger';
import {getProfileOrDefaultToActive} from '@/core/auth/authProfileSelector';
import {safeCheckProfileActive} from '@/core/auth/authValidChecker';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { LaunchingStatus, useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { useAccountsStore } from '@/store/accountsStore.ts';

export type SaveJson = {
  name: string;
  jvmArgs: string;
  jrePath: string;
  programArgs: string;
  memory: number;
  width: number;
  height: number;
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
    return this._play(profileUuid, null);
  }
  
  async playOffline(offlineUsername: string | null = null) {
    return this._play(null, offlineUsername);
  }
  
  private async _play(profileUuid: string | null = null, offlineUsername: string | null = null) {
    const runningInstancesStore = useRunningInstancesStore();
    
    InstanceController.logger.debug(`Playing instance ${this.instance.uuid}`);
    
    let activeProfile;
    if (!offlineUsername) {
      InstanceController.logger.debug("Fetching active profile");
      activeProfile = getProfileOrDefaultToActive(profileUuid);
      if (!activeProfile) {
        InstanceController.logger.warn("Failed to get active profile");
        return;
      }
    }
    
    const loadingStatus: LaunchingStatus = {
      uuid: this.instance.uuid,
      loggingIn: true,
      starting: false,
      error: "",
      step: "Checking profile",
    }
    
    runningInstancesStore.updateLaunchingStatus(loadingStatus)

    if (!offlineUsername && activeProfile) {
      InstanceController.logger.debug("Checking profile");
      const checkResult = await safeCheckProfileActive(activeProfile.uuid);
      if (checkResult !== "VALID") {
        InstanceController.logger.warn("Failed to check profile");

        if (checkResult === "TOTAL_FAILURE" || checkResult === "NOT_LOGGED_IN") {
          if (checkResult === "NOT_LOGGED_IN") {
            InstanceController.logger.debug("Profile is not logged in, asking the user to sign in");
          }
          
          const accountsStore = useAccountsStore();
          loadingStatus.loggingIn = false;
          loadingStatus.error = "Failed to check profile";
          runningInstancesStore.clearLaunchingStatus()
          accountsStore.openSignIn()
          return;
        }

        return;
      }
    }
    
    loadingStatus.loggingIn = false;
    loadingStatus.starting = true;
    loadingStatus.step = "Starting instance";
    runningInstancesStore.updateLaunchingStatus(loadingStatus)

    InstanceController.logger.debug("Sending launch handshake");
    const result = await sendMessage("launchInstance", {
      uuid: this.instance.uuid,
      extraArgs: "",
      offline: !!offlineUsername,
      offlineUsername: offlineUsername || "",
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
      runningInstancesStore.updateLaunchingStatus(loadingStatus)
      return;
    }

    runningInstancesStore.clearLaunchingStatus()
    
    InstanceController.logger.debug("Navigating to running instance");
    await safeNavigate(RouterNames.ROOT_RUNNING_INSTANCE, {uuid: this.instance.uuid});
  }
  
  async updateInstance(data: SaveJson) {
    const instancesStore = useInstanceStore();
    InstanceController.logger.debug("Updating instance", data);
    const result = await sendMessage("instanceConfigure", {
      uuid: this.instance.uuid,
      instanceJson: JSON.stringify(data)
    })

    if (result.status === "success") {
      instancesStore.updateInstance(result.instanceJson)
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
      const instanceStore = useInstanceStore();
      instanceStore.removeInstance(this.instance.uuid)
      return true;
    }
    
    InstanceController.logger.warn("Failed to delete instance", result);
    return false;
  }
}