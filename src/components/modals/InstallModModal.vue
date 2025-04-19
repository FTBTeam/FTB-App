<script lang="ts" setup>
import { Selection2, Modal, ModalBody, ModalFooter, UiButton } from '@/components/ui';
import {getColorForReleaseType, prettyByteFormat} from '@/utils';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Mod} from '@/types';
import {BaseData, InstanceJson, OperationProgressUpdateData} from '@/core/types/javaApi';
import {compatibleCrossLoaderPlatforms} from '@/utils/helpers/packHelpers';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/appStore.ts';
import { faCheck, faDownload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

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
  const crossLoaderPlatforms = compatibleCrossLoaderPlatforms(mcVersion, modLoader);
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
       <div class="py-6" v-if="!installing && !finishedInstalling">
         <div class="flex gap-4 mb-4">
           <FontAwesomeIcon :icon="faDownload" size="xl" />
           <b class="text-lg block">Install {{ mod.name }}</b>
         </div>
         
         <selection2
           label="Select mod version"
           v-model="selectedVersion"
           :options="options"
         />
       </div>

       <div v-if="!installing && !finishedInstalling" class="pt-8">
         <div class="flex items-center gap-4 mb-6">
           <img src="../../assets/curse-logo.svg" alt="CurseForge logo" width="40" />
           <b class="text-lg block">About mod installs</b>
         </div>
         <hr class="curse-border block mb-4" />
         <p class="text-muted mb-4">🎉 Each mod install from the FTB App directly supports the mod developers through the CurseForge reward system!</p>
         <b class="block mb-2">Dependencies</b>
         <p class="mb-2 text-muted">If the mod depends on other mods on the CurseForge platform, the app will try and resolve these dependencies for you and install them as well.</p>
         <p class="text-muted">Sometimes this does not work and you'll have to install the dependencies manually.</p>
       </div>

       <div class="installing mt-6 mb-4" v-if="!finishedInstalling && installing">
         <div class="progress font-bold"><font-awesome-icon icon="spinner" spin class="mr-2" /> Installing</div>
         <div class="stats">
           <div class="stat">
             <div class="text">Progress</div>
             <div class="value">{{ installProgress.percentage }}%</div>
           </div>
         </div>
       </div>

       <p v-if="!installing && finishedInstalling">
         <span class="block">{{ mod.name }} has been installed!</span>
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