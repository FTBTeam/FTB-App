import {
  CancelInstallInstanceDataReply,
  InstallInstanceDataReply,
  InstanceJson,
  InstanceOverrideModLoaderDataReply,
  OperationProgressUpdateData,
  Stage,
  SugaredInstanceJson
} from '@/core/types/javaApi';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import appPlatform from '@platform';
import {createLogger} from '@/core/logger';
import { useInstallStore } from '@/store/installStore.ts';
import { PackProviders, Versions } from '@/core/types/appTypes.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { EmitEvents } from '@/store/appStore.ts';
import { Emitter } from 'mitt';
import { artworkFileOrElse } from '@/utils/helpers/packHelpers.ts';

export type InstallRequest = {
  uuid: string;
  id: string | number;
  version: string | number;
  name: string;
  logo: string | null;
  updatingInstanceUuid?: string;
  importFrom?: string;
  category?: string;
  provider?: PackProviders;
  private: boolean;
  ourOwn?: boolean;
  mcVersion?: string;
  ram?: number;
  width?: number;
  height?: number;
  fullscreen?: boolean;
}

export type InstallStatus = {
  request: InstallRequest,
  stage: string;
  progress: string;
  forInstanceUuid: string;
  speed?: number
}

type InstallResult = {
  success: boolean;
  shouldRemove?: boolean;
  cancel?: boolean;
  message?: string;
  instance?: SugaredInstanceJson;
}

const betterStageNames: Map<Stage, string> = new Map([
  ['NOT_STARTED', 'Not started'],
  ['PREPARE', 'Preparing'],
  ['MOD_LOADER', 'Mod loader'],
  ['FILES', 'Downloading'],
  ['FINISHED', 'Finishing'],
  ['POST_UPDATE', 'Checking'],
  ['SYNC_DOWN', 'Syncing'],
  ['SYNC_UP', 'Syncing'],
  ['CLEAN', 'Cleaning'],
  ['INDEXING_LOCAL', 'Indexing'],
  ['INDEXING_REMOTE', 'Indexing'],
  ['COMPUTING_CHANGES', 'Checking'],
  ['BEGIN_SYNC', 'Syncing'],
  ['POST_UPDATE', 'Validating'],
  ['POST_RUN', 'Validating'],
]);

/**
 * Instance install controller backed by a vuex store queue
 */
export class InstanceInstallController {
  private logger = createLogger('InstanceInstallController.ts');
  private installLock = false;

  constructor(
    private readonly emitter: Emitter<EmitEvents>
  ) {
    this.emitter.on('ws/message', async (data: any) => {
      if (data.type === 'instanceOverrideModLoaderReply') {
        const typedData = data as InstanceOverrideModLoaderDataReply;
        this.handleOverrideState(typedData);
      }
    });

    // Force the queue to be checked on startup
    this.checkQueue()
      .catch(e => this.logger.error(e));

    setInterval(() => {
      this.checkQueue()
        .catch(e => this.logger.error(e));
    }, 5000); // 5 seconds
  }

  public async requestInstall(request: Omit<InstallRequest, 'uuid'>) {
    this.logger.debug('Install requested', request);

    this.queue.push({
      uuid: appPlatform.utils.crypto.randomUUID(),
      ...request,
    });

    // Trigger a check queue
    await this.checkQueue();
    alertController.info(`${request.name} queued for installation`);
  }

  public async requestUpdate(instance: SugaredInstanceJson | InstanceJson, version: Versions | string | number, provider: PackProviders = 'modpacksch') {
    this.logger.debug('Update requested', instance, version, provider);

    this.queue.push({
      uuid: appPlatform.utils.crypto.randomUUID(), // Not the same as the instance uuid
      id: instance.id,
      version: typeof version === 'object' ? version.id : version,
      name: instance.name,
      logo: artworkFileOrElse(instance as SugaredInstanceJson),
      updatingInstanceUuid: instance.uuid,
      category: instance.category,
      provider,
      private: instance._private,
    });

    // Trigger a check queue
    await this.checkQueue();
    alertController.info(`Update requested for ${instance.name}`);
  }

  public async requestImport(path: string, category: string) {
    this.logger.debug('Import requested', path, category);

    this.queue.push({
      uuid: appPlatform.utils.crypto.randomUUID(),
      id: -1,
      version: -1,
      name: 'Import',
      logo: null,
      category: category,
      private: false,
      provider: 'modpacksch',
      importFrom: path,
    });

    // Trigger a check queue
    await this.checkQueue();
    alertController.info(`Import requested for ${path.split('/').pop()}`);
  }

  public async cancelInstall(uuid: string, isInstall = false) {
    this.logger.debug('Cancel requested', uuid);
    const installStore = useInstallStore()

    if (isInstall) {
      // Get the current install request
      const currentInstall = installStore.currentInstall;
      if (currentInstall == null) {
        return;
      }

      // Failsafe
      if (currentInstall.request.uuid !== uuid) {
        return;
      }

      // Cancel the install
      let tried = 0;
      let result: null | CancelInstallInstanceDataReply = null;
      while (++tried < 3) {
        try {
          result = await sendMessage('cancelInstallInstance', {
            uuid: currentInstall.forInstanceUuid
          }, 10_000);

          if (result.status === 'success') {
            break;
          }
        } catch (e) {
          this.logger.error('Failed to cancel install', e);
        }
      }

      if (result == null || result.status !== 'success') {
        alertController.error(`Failed to cancel install for ${currentInstall.request.name}`);
        return;
      }
    }

    const index = this.queue.findIndex(e => e.uuid === uuid);
    if (index === -1) {
      return;
    }

    alertController.info(`Cancelled install for ${this.queue[index].name}`);
    this.queue.splice(index, 1);
  }

  private async checkQueue() {
    const installStore = useInstallStore()
    if (this.queue.length === 0 || this.installLock) {
      return;
    }

    return;
    
    this.logger.debug('Checking queue');
    this.logger.debug('Queue items', this.queue.length);
    const request = installStore.popInstallQueue()
    if (request == null) {
      this.logger.debug('Queue is empty');
      return;
    }

    // Don't holt the queue while we install
    this.installPack(request)
      .catch(e => this.logger.error(e));
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
      return;
    }

    this.installLock = true;
    const isUpdate = request.updatingInstanceUuid != null;

    let payload: any = {};

    if (!request.importFrom) {
      payload = {
        uuid: request.updatingInstanceUuid ?? '', // This flag is what tells the API to update an instance
        id: parseInt(request.id as string, 10),
        version: parseInt(request.version as string, 10),
        _private: request.private,
        packType: !request.provider ? 0 : (request.provider === 'modpacksch' ? 0 : 1),
        importFrom: null,
        name: request.name,
        artPath: request.logo,
        category: request.category ?? 'Default',
        mcVersion: request.mcVersion ?? undefined,
        ourOwn: request.ourOwn ?? false,
        screenWidth: request.width ?? -1,
        screenHeight: request.height ?? -1,
        fullscreen: request.fullscreen ?? null,
        ram: request.ram ?? -1,
      };
    } else if (request.importFrom) {
      payload = {
        importFrom: request.importFrom,
        category: request.category ?? 'Default',
      };
    }

    this.logger.debug('Sending install request', payload);

    let knownInstanceUuid = '';
    let continuePast = false;
    const instanceStore = useInstanceStore();

    // Make the installation request!
    this.logger.debug('Attempting to install instance');
    try {
      const installResponse = await sendMessage('installInstance', payload);

      if (installResponse.status === 'error' || installResponse.status === 'prepare_error') {
        this.logger.debug('Failed to send install request', installResponse);
        alertController.error(`Failed to start installation due to ${installResponse.message ?? 'an unknown error'}`);
        this.installLock = false;
        return;
      }

      if (installResponse.instanceData && !isUpdate) {
        // Don't add if it's an update otherwise we'll have two instances
        instanceStore.addInstance(installResponse.instanceData)
      }

      knownInstanceUuid = installResponse.instanceData.uuid;
      continuePast = true;
    } catch (error) {
      this.logger.error('Failed to send install request', error);
    }

    if (!continuePast) {
      this.installLock = false;
      this.logger.debug('Unlocking install lock due to error');
      return;
    }

    const installRequest: InstallResult = await new Promise((resolve) => {
      this.logger.debug('Preparing install listener');
      this.updateInstallStatus({
        request,
        progress: '0',
        stage: 'Starting install',
        forInstanceUuid: knownInstanceUuid
      });

      let lastKnownProgress: string | undefined = undefined;
      let lastKnownStage: string | undefined = undefined;
      let lastKnownSpeed: number = 0;

      const instanceInstaller = (data: InstallInstanceDataReply | OperationProgressUpdateData) => {
        const finish = (result: InstallResult) => {
          this.emitter.off('ws/message', instanceInstaller as any);
          this.updateInstallStatus(null);
          resolve(result);
        };

        if (data.type === 'installInstanceDataReply') {
          const typedData = data as InstallInstanceDataReply;
          if (typedData.status === 'error') {
            return finish({
              success: false,
              message: typedData.message,
              shouldRemove: !isUpdate
            });
          } else if (typedData.status === 'success') {
            if (typedData.message === "Triggered cancellation.") {
              return finish({
                success: false,
                cancel: true,
                // Check if it's an update or not. If it's not, we should remove it
                shouldRemove: !isUpdate
              });
            }
            
            return finish({
              success: true,
              instance: typedData.instanceData as SugaredInstanceJson, // It's not really but it's fine
            });
          } else if (typedData.status === 'init') {
            lastKnownStage = 'Initializing';
            lastKnownProgress = '0';
          } else if (typedData.status === 'canceled') {
            return finish({
              success: false,
              cancel: true,
              shouldRemove: !isUpdate
            });
          }
        } else if (data.type === 'operationUpdate') {
          const typedData = data as OperationProgressUpdateData;
          if (typedData.metadata.instance !== knownInstanceUuid) {
            return;
          }

          if (typedData.stage === 'FINISHED') {
            return finish({
              success: true
            });
          }

          lastKnownStage = betterStageNames.get(typedData.stage) ?? toTitleCase(typedData.stage as string);
          lastKnownProgress = typedData.percent.toFixed(1);
          lastKnownSpeed = typedData.speed;
        }

        if (data.type === 'installInstanceDataReply' || data.type === 'operationUpdate') {
          this.updateInstallStatus({
            request,
            stage: lastKnownStage ?? '',
            progress: lastKnownProgress ?? '',
            forInstanceUuid: knownInstanceUuid,
            speed: lastKnownSpeed
          });
        }
      };
      
      this.emitter.on('ws/message', instanceInstaller as any);
    });

    this.logger.debug('Install result', installRequest);
    if (installRequest.success && installRequest.instance) {
      // Success! We always update as we've already added the instance to the store, this will toggle the installed state for the card.
      instanceStore.updateInstance(installRequest.instance)
      alertController.success(`Successfully installed ${request.name}`);
    } else if (installRequest.success && !installRequest.instance) {
      alertController.success(`Successfully synced ${request.name}`);
      instanceStore.loadInstances();
    } else if (!installRequest.success && installRequest.cancel) {
      alertController.info(`Cancelled install for ${request.name}`);
    } else {
      alertController.error(`Failed to install ${request.name} due to an unknown error`);
      if (knownInstanceUuid && installRequest.shouldRemove) {
        instanceStore.removeInstance(knownInstanceUuid)
      }
    }

    this.logger.debug('Unlocking install lock');
    this.installLock = false;
    await this.checkQueue(); // Force a queue check again 
  }

  private updateInstallStatus(status: InstallStatus | null) {
    const installStore = useInstallStore()
    installStore.currentInstall = status;
  }

  private get queue() {
    const installStore = useInstallStore()
    return installStore.installQueue;
  }

  /**
   * Handles modloader overloading state updating
   */
  private handleOverrideState(typedData: InstanceOverrideModLoaderDataReply) {
    const installStore = useInstallStore()
    
    const status = typedData.status;
    if (status === 'prepare') {
      this.logger.debug('Preparing modloader update, unable override the state');
      return;
    }

    if (status === 'error' || status === 'success') {
      const packetId = typedData.requestId;
      const instanceStore = useInstanceStore();

      const updateFromPacketId = installStore.currentModloaderUpdate.find(e => e.packetId === packetId);
      if (!updateFromPacketId) {
        return;
      }

      const instance = instanceStore.instances.find(e => e.uuid === updateFromPacketId.instanceId);

      // remove the modloader update
      installStore.removeModloaderUpdate(packetId)

      if (status === 'error') {
        alertController.error(`Failed to update modloader due to ${typedData.message ?? 'an unknown error'}`);
      } else {
        if (instance) {
          alertController.success(`Successfully updated modloader for ${instance.name}`);
        } else {
          alertController.success(`Successfully updated modloader`);
        }
      }
    }
  }
}