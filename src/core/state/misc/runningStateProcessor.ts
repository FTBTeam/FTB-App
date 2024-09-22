import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import {emitter} from '@/utils';
import {ClientLaunchDataReply, LaunchInstanceDataReply, Logs, Status, Stopped} from '@/core/@types/javaApi';
import store from '@/modules/store';
import {createLogger} from '@/core/logger';
import {InstanceMessageData, RunningState} from '@/core/state/misc/runningState';

const logger = createLogger("runningStateProcessor.ts");

const typeMap = new Map([
  ["WARN", "W"],
  ["INFO", "I"],
  ["ERROR", "E"],
  ["DEBUG", "D"],
  ["TRACE", "T"],
])

const lastMessageTypeByUuid = new Map<string, string>()
export const lastMessageIndexByUuid = new Map<string, number>()

async function onLaunchData(data: any) {
  if (data.type === 'launchInstance.logs') {
    const typedData = data as Logs;

    const instanceUuid = typedData.uuid;
    
    gobbleError(() => lazyLogChecker(instanceUuid, data.messages)).catch(console.error);
    const messages = typedData.messages
      .map(e => formatMessage(instanceUuid, e))
      .filter(e => e !== undefined && e !== null) as InstanceMessageData[];
    
    await store.dispatch('v2/running/pushMessages', {uuid: instanceUuid, messages: messages});
  }

  if (data.type === 'launchInstance.status') {
    const typedData = data as Status;
    const instanceUuid = typedData.uuid;
    
    await store.dispatch('v2/running/updateStatus', {
      uuid: instanceUuid, 
      step: {
        desc: typedData.stepDesc,
        totalSteps: typedData.totalSteps,
        step: typedData.step,
        progress: typedData.stepProgress,
        progressHuman: typedData.stepProgressHuman,
      }
    });
  }

  if (data.type === 'clientLaunchData') {
    const typedData = data as ClientLaunchDataReply;
    const instance = typedData.instance;
    
    const currentInstance = (store.state["v2/running"] as RunningState).instances.find((i) => i.uuid === instance);
    if (!currentInstance) {
      return;
    }
    
    logger.info("Client launch data", typedData)
    if (typedData.messageType === 'message') {
      await store.dispatch('v2/running/updateBars', {
        uuid: instance,
        bars: typedData.message === 'init' ? [] : undefined
      });
    } else if (typedData.messageType === 'progress') {
      if (typedData.clientData.bars) {
        await store.dispatch('v2/running/updateBars', {
          uuid: instance,
          bars: typedData.clientData.bars
        });
      }
    } else if (typedData.messageType === 'clientDisconnect' || (typedData.messageType === 'message' && typedData.message === 'done')) {
      console.log("Disconnecting client", instance)
      await store.dispatch('v2/running/finishedLoading', instance);
    }
  }

  if (
    data.type === 'launchInstance.stopped' ||
    (data.type === 'launchInstance.reply' && (data.status === 'abort' || data.status === 'error'))
  ) {
    if (data.type === "launchInstance.stopped") {
      const typeData = data as Stopped;
      await store.dispatch('v2/running/stopped', typeData.instanceId);
      return;
    }
    
    // Lets assume we've crashed
    if (data.status === 'errored' || data.status === 'error') {
      alertController.error(data.status === 'error'
        ? 'Unable to start pack... please see the instance logs...'
        : 'The instance has crashed or has been externally closed.'
      )

      const uuid = (data as Stopped).instanceId ?? (data as LaunchInstanceDataReply).uuid;
      await store.dispatch('v2/running/crashed', uuid);
    }
  }
}

function formatMessage(uuid: string, message: string): InstanceMessageData | undefined {
  const instance = (store.state["v2/running"] as RunningState).instances.find((instance) => instance.uuid === uuid);
  if (!instance) {
    return;
  }
  
  let type = lastMessageTypeByUuid.get(uuid) ?? "I";
  const lastMessageIndex = lastMessageIndexByUuid.get(uuid) ?? -1;
  
  const result = /^\[[^\/]+\/([^\]]+)]/.exec(message);
  if (result !== null && result[1]) {
    type = typeMap.get(result[1]) ?? "I";
  }

  lastMessageIndexByUuid.set(uuid, lastMessageIndex + 1);
  lastMessageTypeByUuid.set(uuid, type);
  
  return {i: lastMessageIndex + 1, t: type, v: message};
}

async function lazyLogChecker(uuid: string, messages: string[]) {
  const stateData = (store.state["v2/running"] as RunningState).instances.find((instance) => instance.uuid === uuid);
  if (stateData?.status.finishedLoading) {
    return;
  }

  // Make an educated guess that when this shows, we're likely good to assume it's loaded
  for (const message of messages) {    
    if (message.includes("Created:") && message.includes("minecraft:textures/atlas")) {
      console.log("Created assets", uuid)
      await store.dispatch('v2/running/finishedLoading', uuid);
    }

    // This also tends to happen relatively late in the startup process so we can use it as a marker as well
    if (message.includes("Sound engine started")) {
      console.log("Sound engine started", uuid)
      await store.dispatch('v2/running/finishedLoading', uuid);
    }
  }
}

export function initStateProcessor() {
  emitter.on('ws.message', onLaunchData);
}