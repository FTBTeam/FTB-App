<script lang="ts" setup>
import {Modal, ModalBody, ModalFooter, UiButton, UiBadge, ProgressBar} from '@/components/ui';
import {getColorForReleaseType, prettyByteFormat} from '@/utils';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Mod} from '@/types';
import {BaseData, InstanceJson, OperationProgressUpdateData} from '@/core/types/javaApi';
import {compatibleCrossLoaderPlatforms} from '@/utils/helpers/packHelpers';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/appStore.ts';
import {faBolt, faDownload, faSpinner} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import {alertController} from "@/core/controllers/alertController.ts";
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {toTitleCase} from "@/utils/helpers/stringHelpers.ts";

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
const selectedVersion = ref<string>("");

const installProgress = ref<InstallProgress>(emptyProgress);
const wsReqId = ref("");

function close() {
  installProgress.value = emptyProgress;
  installing.value = false;
  selectedVersion.value = "";
  emit('close')
}

onMounted(() => {
  appStore.emitter.on('ws/message', onInstallMessage);
})

onUnmounted(() => {
  // Stop listening to events!
  appStore.emitter.off('ws/message', onInstallMessage);
})

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
    
    if (data.status !== 'success') {
      return;
    }
    
    wsReqId.value = "";
    installing.value = false;
    installProgress.value = emptyProgress;
    emit('installed')
    alertController.success("Mod installed successfully!");
    close();
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
  selectedVersion.value = "";
}

const options = computed(() => {
  if (!mod) {
    return [];
  }
  
  const crossLoaderPlatforms = compatibleCrossLoaderPlatforms(mcVersion, modLoader.toLowerCase());
  return (mod.versions
    // This needs to either, check it's a compatible loader, OR check if the mod has no loader, and if it's got no loader, assume it's for forge, and then check if the modloader is forge
    .filter(e => e.targets.findIndex((a) => a.type === 'modloader' && crossLoaderPlatforms.includes(a.name)) !== -1 || (e.targets.findIndex(a => a.type === 'modloader') === -1 && modLoader.toLowerCase() === 'forge'))
    .filter(e => e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === mcVersion) !== -1)
    .sort((a, b) => b.id - a.id)
    .map((e) => ({
      key: e.id.toString(),
      value: e.name,
      size: e.size,
      releaseType: e.type,
    })) ?? []) as UiSelectOption<{size: number, releaseType: string}>[];
});

watch([() => open, () => options.value], (updateOptions) => {
  if (!updateOptions[0] || !updateOptions[1]) {
    return;
  }

  selectedVersion.value = options.value[0]?.key as any;
});

const modLogo = computed(() => {
  if (!mod) return;
  
  return mod.art.find(e => e.type === "default")?.url;
})

const modLink = computed(() => {
  if (!mod) return undefined;
  
  return mod.links.find(e => e.type === "curseforge")?.link;
})
</script>

<template>
   <modal :open="open" @closed="close" :close-on-background-click="!installing"
          title="Install mod" :sub-title="installing ? 'Installing' : `Select the version you want to install`" :external-contents="true"
   >
     <modal-body v-if="mod">
       <div class="mt-4 mb-4 flex gap-6 items-center">
         <div class="bg-white/5 border border-white/10 inline-block rounded-lg p-2">
           <img :src="modLogo" class="max-h-[80px]" />
         </div>
         
         <div>
           <a :href="modLink" class="font-bold text-xl hover:underline">{{ mod.name }}</a>
           <p class="line-clamp-2 mt-1">{{ mod.synopsis }}</p>
           <p class="text-white/80 mt-2">By <b>{{ mod.authors.map(e => e.name).join(", ") }}</b></p>
         </div>
       </div>
       
       <div class="py-4" v-if="!installing">
         <UiSelect :options="options" v-model="selectedVersion" label="Mod version">
           <template #option="{ option, clazz }">
             <div class="flex items-center gap-4" :class="clazz">
               <UiBadge class="!font-bold text-shadow" :style="{ backgroundColor: `${getColorForReleaseType(option.releaseType)}` }">{{ toTitleCase(option.releaseType) }}</UiBadge>
               <div class="flex-1">{{ option.value }}</div>
               <div class="text-right ml-6 text-white/80">
                 <span v-if="option.size">{{ prettyByteFormat(option.size) }}</span>
                 <span v-else>Unknown size</span>
               </div>
             </div>
           </template>
         </UiSelect>
       </div>

       <div v-if="!installing" class="pt-8">
         <div class="flex items-center gap-4 mb-6">
           <img src="../../assets/curse-logo.svg" alt="CurseForge logo" width="26" />
           <b class="block text-lg">About mod installs</b>
         </div>
         <ul class="flex flex-col gap-3 list-disc pl-4">
           <li>❤️ Each mod install directly supports CurseForge Developers!</li>
           <li>🛠️ We'll do our best to install mod dependencies for you</li>
         </ul>
       </div>

       <div class="installing mt-6 mb-4" v-if="installing">
         <div class="flex items-center justify-between">
           <div class="progress font-bold text-lg">
             <font-awesome-icon :icon="faSpinner" spin class="mr-2" /> Installing
           </div>
           <div>
             <FontAwesomeIcon :icon="faBolt" class="mr-2" />{{(installProgress.speed / 12500000).toFixed(2)}} Mbps
           </div>
         </div>
         <ProgressBar class="mt-4" :progress="installProgress.percentage / 100" />
       </div>
     </modal-body>

     <modal-footer>
       <div class="flex justify-end gap-4" v-if="!installing">
         <ui-button type="success" :icon="faDownload" v-if="!installing" :disabled="!selectedVersion" @click="installMod">Install</ui-button>
       </div>
     </modal-footer>
   </modal>
</template>