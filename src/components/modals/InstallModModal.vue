<script lang="ts" setup>
import { Selection2, Modal, ModalBody, ModalFooter, UiButton } from '@/components/ui';
import {getColorForReleaseType, prettyByteFormat} from '@/utils';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Mod} from '@/types';
import {BaseData, InstanceJson, OperationProgressUpdateData} from '@/core/types/javaApi';
import {compatibleCrossLoaderPlatforms} from '@/utils/helpers/packHelpers';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/appStore.ts';
import {faCheck, faDownload, faSpinner} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import {alertController} from "@/core/controllers/alertController.ts";

type InstallProgress = {
  percentage: number;
  speed: number;
  current: number;
  total: number;
};

const {
  mod,
  instance,
  mcVersion,
  modLoader,
  open
} = defineProps<{
  mod?: Mod;
  instance?: InstanceJson;
  mcVersion: string;
  modLoader: string;
  open: boolean;
}>();

const emptyProgress = {
  percentage: 0,
  speed: 0,
  current: 0,
  total: 0,
};

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'installed'): void;
}>();

const appStore = useAppStore();

const installing = ref(false);
const finishedInstalling = ref(false);
const selectedVersion = ref<string | null>(null);

const installProgress = ref<InstallProgress>(emptyProgress);
const wsReqId = ref("");

function close() {
  installProgress.value = emptyProgress;
  installing.value = false;
  finishedInstalling.value = false;
  selectedVersion.value = null;
  emit('close')
}

onMounted(() => {
  appStore.emitter.on('ws/message', onInstallMessage);
})

onUnmounted(() => {
  // Stop listening to events!
  appStore.emitter.off('ws/message', onInstallMessage);
})

watch(() => open, (newVal) => {
  if (!newVal) return;
  selectedVersion.value = options.value[0]?.value as any;
});

function onInstallMessage(data: BaseData & { [key: string]: any }) {
  if (data.type !== "operationUpdate" && data.type !== "instanceInstallModReply") {
    return;
  }

  if (data.type === 'instanceInstallModReply') {
    if (data.status === 'error') {
      alertController.error("Error installing mod: " + data.message);
      wsReqId.value = "";
      installing.value = false;
      installProgress.value = emptyProgress;
      return;
    }
    
    wsReqId.value = "";
    installing.value = false;
    installProgress.value = emptyProgress;
    finishedInstalling.value = true;
    emit('installed')
    return;
  }

  // Handle progress
  if (data.type === 'operationUpdate') {
    const typedData = data as OperationProgressUpdateData;
    if (typedData.metadata.instance !== instance!.uuid) {
      return;
    }

    installProgress.value = {
      percentage: typedData.percent,
      speed: typedData.speed,
      current: typedData.bytes,
      total: typedData.totalBytes,
    };
  }
}

async function installMod() {
  if (!selectedVersion.value) {
    return;
  }

  installing.value = true;
  const result = await sendMessage("instanceInstallMod", {
    uuid: instance!.uuid,
    modId: mod!.id,
    versionId: parseInt(selectedVersion.value, 10),
  })

  wsReqId.value = result.messageId;
  selectedVersion.value = null;
}

const options = computed(() => {
  const crossLoaderPlatforms = compatibleCrossLoaderPlatforms(mcVersion, modLoader.toLowerCase());
  return mod!.versions
    .filter(e => e.targets.findIndex(a => a.type === 'modloader' && crossLoaderPlatforms.includes(a.name)) !== -1)
    .filter(e => e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === mcVersion) !== -1)
    .sort((a, b) => b.id - a.id)
    .map((e) => ({
      value: e.id,
      label: e.name,
      badge: {
        text: e.type,
        color: getColorForReleaseType(e.type),
      },
      meta: prettyByteFormat(e.size),
    })) ?? []
});
</script>obon

<template>
   <modal :open="open" @closed="close" :close-on-background-click="!installing"
          :title="mod ? mod.name : 'Loading...'" :sub-title="!installing && finishedInstalling ? 'Installed!' : `Select the version you want to install`" :external-contents="true"
   >
     <modal-body v-if="mod">
       <div class="py-4" v-if="!installing && !finishedInstalling">
         <selection2
           label="Select mod version"
           v-model="selectedVersion"
           :options="options"
         />
       </div>

       <div v-if="!installing && !finishedInstalling" class="pt-8">
         <div class="flex items-center gap-4 mb-4">
           <img src="../../assets/curse-logo.svg" alt="CurseForge logo" width="26" />
           <b class="block">About mod installs</b>
         </div>
         <ul class="flex flex-col gap-3">
           <li>❤️ Each mod install directly supports CurseForge Developers!</li>
           <li>🛠️ We'll do our best to install mod dependencies for you</li>
         </ul>
       </div>

       <div class="installing mt-6 mb-4" v-if="!finishedInstalling && installing">
         <div class="progress font-bold"><font-awesome-icon :icon="faSpinner" spin class="mr-2" /> Installing</div>
         <div class="stats">
           <div class="stat">
             <div class="text">Progress</div>
             <div class="value">{{ installProgress.percentage }}%</div>
           </div>
         </div>
       </div>

       <p v-if="!installing && finishedInstalling">
         <span class="block">Mod has been installed! Thank you for support CurseForge developers ❤️</span>
       </p>
     </modal-body>

     <modal-footer>
       <div class="flex justify-end gap-4" v-if="!installing || !finishedInstalling">
         <ui-button type="success" :icon="faDownload" v-if="!installing && !finishedInstalling" :disabled="!selectedVersion" @click="installMod">Install</ui-button>
         <ui-button type="primary" @click="close" :icon="faCheck" v-if="finishedInstalling">Close</ui-button>
       </div>
     </modal-footer>
   </modal>
</template>