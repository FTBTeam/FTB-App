import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import {ClientLaunchDataReply, LaunchInstanceDataReply, Logs, Status, Stopped} from '@/core/types/javaApi';
import {createLogger} from '@/core/logger';
import { InstanceMessageData, useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useAppStore } from '@/store/appStore.ts';

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
  const runningInstancesStore = useRunningInstancesStore();
  
  if (data.type === 'launchInstance.logs') {
    const typedData = data as Logs;

    const instanceUuid = typedData.uuid;
    
    gobbleError(() => lazyLogChecker(instanceUuid, data.messages)).catch(console.error);
    const messages = typedData.messages
      .map(e => formatMessage(instanceUuid, e))
      .filter(e => e !== undefined && e !== null) as InstanceMessageData[];

    runningInstancesStore.pushMessages(instanceUuid, messages)
  }

  if (data.type === 'launchInstance.status') {
    const typedData = data as Status;
    const instanceUuid = typedData.uuid;
    
    runningInstancesStore.updateStatus(instanceUuid, {
      desc: typedData.stepDesc,
      totalSteps: typedData.totalSteps,
      step: typedData.step,
      progress: typedData.stepProgress,
      progressHuman: typedData.stepProgressHuman,
    })
  }

  if (data.type === 'clientLaunchData') {
    const typedData = data as ClientLaunchDataReply;
    const instance = typedData.instance;
    
    const currentInstance = runningInstancesStore.instances.find(e => e.uuid == instance)
    if (!currentInstance) {
      return;
    }
    
    logger.info("Client launch data", typedData)
    if (typedData.messageType === 'message') {
      runningInstancesStore.updateBars(instance, typedData.message === 'init' ? [] : undefined)
    } else if (typedData.messageType === 'progress') {
      runningInstancesStore.updateBars(instance, typedData.clientData.bars)
    } else if (typedData.messageType === 'clientDisconnect' || (typedData.messageType === 'message' && typedData.message === 'done')) {
      console.log("Disconnecting client", instance)
      runningInstancesStore.finishedLoading(instance)
    }
  }

  if (
    data.type === 'launchInstance.stopped' ||
    (data.type === 'launchInstance.reply' && (data.status === 'abort' || data.status === 'error'))
  ) {
    if (data.type === "launchInstance.stopped") {
      const typeData = data as Stopped;
      runningInstancesStore.stopped(typeData.instanceId)
      return;
    }
    
    // Lets assume we've crashed
    if (data.status === 'errored' || data.status === 'error') {
      alertController.error(data.status === 'error'
        ? 'Unable to start pack... please see the instance logs...'
        : 'The instance has crashed or has been externally closed.'
      )

      const uuid = (data as Stopped).instanceId ?? (data as LaunchInstanceDataReply).uuid;
      runningInstancesStore.crashed(uuid)
      runningInstancesStore.stopped(uuid)
    }
  }
}

function formatMessage(uuid: string, message: string): InstanceMessageData | undefined {
  const runningInstancesStore = useRunningInstancesStore();
  const instance = runningInstancesStore.instances.find((instance) => instance.uuid === uuid);
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
  const runningInstancesStore = useRunningInstancesStore();
  const storeData = runningInstancesStore.instances.find(instance => instance.uuid === uuid);
  if (storeData?.status.finishedLoading) {
    return;
  }

  // Make an educated guess that when this shows, we're likely good to assume it's loaded
  for (const message of messages) {    
    if (message.includes("Created:") && message.includes("minecraft:textures/atlas")) {
      console.log("Created assets", uuid)
      runningInstancesStore.finishedLoading(uuid);
    }

    // This also tends to happen relatively late in the startup process so we can use it as a marker as well
    if (message.includes("Sound engine started")) {
      console.log("Sound engine started", uuid)
      runningInstancesStore.finishedLoading(uuid)
    }
  }
}

export function initStateProcessor() {
  const appStore = useAppStore();
  appStore.emitter.on('ws/message', onLaunchData);
}