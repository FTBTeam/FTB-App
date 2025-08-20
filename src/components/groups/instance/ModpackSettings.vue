<script lang="ts" setup>
import appPlatform from '@platform'
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import { InstanceJson, SugaredInstanceJson } from '@/core/types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import ModloaderSelect from '@/components/groups/modpack/components/ModloaderSelect.vue';
import {ModLoaderWithPackId} from '@/core/types/modpacks/modloaders';
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, UiButton, Input } from '@/components/ui';
import { useInstallStore } from '@/store/installStore.ts';
import {
  faCopy,
  faDownload,
  faFolder, faInfoCircle, faLock,
  faPen, faTimes,
  faTrash, faUnlock, faWarning,
  faWrench,
} from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';
import ResolutionSelector, {ResolutionValue} from "@/components/groups/modpack/components/ResolutionSelector.vue";
import ReleaseChannelSelector from "@/components/groups/modpack/components/ReleaseChannelSelector.vue";
import {packBlacklist} from "@/store/modpackStore.ts";
import InstanceSettings, {InstanceSettingsValue} from "@/components/groups/settings/InstanceSettings.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import RamSlider from "@/components/groups/modpack/components/RamSlider.vue";

const router = useRouter();
const appStore = useAppStore();
const installStore = useInstallStore();

const {
  instance
} = defineProps<{
  instance: SugaredInstanceJson;
}>()

const emit = defineEmits<{
  (event: 'back'): void;
}>()


const instanceSettings = ref<SaveJson>({} as any);
const previousSettings = ref<SaveJson>({} as any);

const deleting = ref(false);

const imageFile = ref<string | null>(null);

const userSelectModLoader = ref(false);
const userSelectedLoader = ref<[string, ModLoaderWithPackId] | null>(null);

const instanceSettingsValue = ref<InstanceSettingsValue | null>(null);

onMounted(async () => {
  instanceSettingsValue.value = {
    java: {
      ram: instance.memory,
      javaPath: instance.jrePath,
    },
    launch: {
      shellArgs: instance.shellArgs,
      javaArgs: instance.jvmArgs,
      programArgs: instance.programArgs,
      disableInjectedMods: instance.preventMetaModInjection,
      disableJavaAgents: instance.preventMetaModInjection, // TODO: Fix this one
    },
    window: {
      fullscreen: instance.fullscreen,
      width: instance.width,
      height: instance.height,
    },
    // TODO: Load the game options from the options.txt file on the instance
    game: {},
  }
  
  instanceSettings.value = createInstanceSettingsFromInstance(instance);
  previousSettings.value = { 
    ...instanceSettings.value
  }
})

function selectResolution(value: ResolutionValue) {
  if (!value) {
    return;
  }

  instanceSettings.value.fullScreen = value.fullScreen;
  instanceSettings.value.width = value.width;
  instanceSettings.value.height = value.height;
  
  saveSettings();
}

async function repairInstance() {
  if (!(await dialogsController.createConfirmationDialog("Are you sure?", "We will attempt to repair this instance by reinstalling the modpack around your existing files. Even though this shouldn't remove any of your data, we recommend you make a backup of this instance before continuing."))) {
    return;
  }

  await appStore.controllers.install.requestUpdate(instance, instance.versionId, typeIdToProvider(instance.packType));
  emit("back")
}

async function installModLoader() {
  if (!userSelectedLoader.value) {
    return;
  }
  
  const result = await sendMessage("instanceOverrideModLoader", {
    uuid: instance.uuid,
    modLoaderId: parseInt(userSelectedLoader.value[1].packId, 10),
    modLoaderVersion: userSelectedLoader.value[1].id,
  });  
  
  if (result.status !== "error") {
    userSelectModLoader.value = false;
    userSelectedLoader.value = null;

    if (result.status === "prepare") {
      installStore.addModloaderUpdate({
        instanceId: instance.uuid,
        packetId: result.requestId
      })
    }

    emit("back");
  }
}

async function toggleLock() {
  const newState = !instanceSettings.value.locked;
  if (!newState && !(await dialogsController.createConfirmationDialog("Are you sure?", "Unlocking this instance will allow you to add extra mods and modify the instance in other ways. This can allow for destructive actions!\n\nAre you sure you want to unlock this instance?"))) {
    return;
  }
  
  instanceSettings.value.locked = newState;
  await saveSettings(); 
}

async function saveSettings() {
  if (JSON.stringify(previousSettings.value) === JSON.stringify(instanceSettings.value)) {
    return;
  }
  
  const result = await InstanceController.from(instance)
    .updateInstance(instanceSettings.value);

  if (result) {
    alertController.success("Settings saved!")

    // Update the previous settings
    instanceSettings.value = createInstanceSettingsFromInstance(result.instanceJson)
    previousSettings.value = {
      ...instanceSettings.value
    }
  } else {
    alertController.error("Failed to save settings")
  }
}

async function browseInstance() {
  await gobbleError(async () => {
    if ("path" in instance) {
      await appPlatform.io.openFinder(instance.path)
      return;
    }
  })
}

function confirmDelete() {
  const dialogRef = dialogsController.createDialog(
    dialog("Are you sure?")
      .withContent(`Are you absolutely sure you want to delete \`${instance.name}\`! Doing this **WILL permanently** delete all mods, world saves, configurations, and all the rest... There is no way to recover this pack after deletion...`)
      .withType("warning")
      .withButton(button("Delete")
        .withAction(async () => {
          dialogRef.setWorking(true)
          await deleteInstance()
          dialogRef.close();
        })
        .withIcon(faTrash)
        .withType("error")
        .build())
      .build()
  )
}

function createInstanceSettingsFromInstance(instance: InstanceJson): SaveJson {
  return {
    name: instance.name,
    jvmArgs: instance.jvmArgs,
    jrePath: instance.jrePath,
    memory: instance.memory,
    width: instance.width,
    height: instance.height,
    fullScreen: instance.fullscreen,
    releaseChannel: instance.releaseChannel,
    categoryId: instance.categoryId,
    locked: instance.locked,
    shellArgs: instance.shellArgs,
    programArgs: instance.programArgs,
    preventMetaModInjection: instance.preventMetaModInjection,
  }
}

async function deleteInstance() {
  deleting.value = true;

  const controller = InstanceController.from(instance);
  await toggleBeforeAndAfter(() => controller.deleteInstance(), state => deleting.value = state);
  await gobbleError(() => router.push({
    name: RouterNames.ROOT_LIBRARY
  }));
}

const hasModloader = computed(() => instance?.modLoader !== instance.mcVersion);

watch(imageFile, (value) => {
  if (value) {
    instanceSettings.value.instanceImage = value
    saveSettings();
  }
})

watch(() => instanceSettings.value.categoryId, (newValue, oldValue) => {
  if (newValue === oldValue) return;
  
  saveSettings()
})

const isLoader = computed(() => {
  if (!instance) return false;
  return packBlacklist.includes(instance.id)
})
</script>

<template>
  <InstanceSettings v-if="instanceSettingsValue" v-model="instanceSettingsValue">
    <template #general>
      <ArtworkSelector :pack="instance" class="mb-4" v-model="imageFile" :allow-remove="false" />

      <div class="flex gap-6 items-center mb-6">
        <Input
          label="Instance Name"
          v-model="instanceSettings.name"
          @blur="saveSettings"
          fill
        />

        <CategorySelector :open-down="true" class="w-2/3" v-model="instanceSettings.categoryId" />
      </div>


      <div class="buttons flex flex-wrap gap-3 mb-8">      
      <UiButton size="small" type="info" :icon="faFolder" @click="browseInstance()">
        Open Folder
      </UiButton>

      <UiButton size="small" :icon="faCopy" @click="InstanceController.from(instance).openDuplicateDialog()" type="info" aria-label="Copy this instance to a new instance, mods, worlds and all">
        Duplicate
      </UiButton>

      <UiButton size="small" type="warning" :icon="instanceSettings.locked ? faUnlock : faLock" @click="toggleLock">
        {{instanceSettings.locked ? 'Unlock' : 'Lock'}} instance
      </UiButton>
      
      <UiButton v-if="instance.id != -1" size="small" :icon="faWrench" type="warning" aria-label="Something not looking right? This might help!" @click="repairInstance">
        Repair
      </UiButton>
      
      <UiButton size="small" type="danger" :icon="faTrash" @click="confirmDelete">
        Delete instance
      </UiButton>
    </div>

    <RamSlider class="mb-8" v-model="instanceSettings.memory" @update="saveSettings" />

    <ResolutionSelector :model-value="{
      fullScreen: instanceSettings.fullScreen,
      width: instanceSettings.width,
      height: instanceSettings.height
    }" @update="selectResolution" />
    
    <h2 class="text-lg mb-6 font-bold text-warning">
      <FontAwesomeIcon :icon="faWarning" class="mr-2" />
      Advanced
    </h2>
    
    <div class="mb-8">
      <div class="flex items-center mb-6" v-if="!instance.isImport && !isLoader">
        <div class="block flex-1 mr-2">
          <b>Release Channel</b>
          <small class="block text-muted mr-6 mt-2">
            The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
        </div>

        <ReleaseChannelSelector v-model="instanceSettings.releaseChannel" @update:modelValue="() => saveSettings()" :include-unset="true" />
      </div>

        <div class="flex items-center mb-6">
          <div class="block flex-1 mr-2" :class="{'opacity-75': instance.locked}">
            <b>Modloader</b>
            <div v-if="instance.locked" class="mt-2 text-red-400">The instances Modloader can not be modified whilst the instance is locked.</div>
            <small class="block text-muted mr-6 mt-2">At any point you can update / down grade your mod loader for any modpack. This can sometimes be a destructive action and we recommend only doing this when you know what you're doing.</small>

            <div class="buttons flex gap-2 mt-4" v-if="!instance.locked">
              <UiButton v-if="!hasModloader" size="small" type="info" :icon="faDownload" @click="userSelectModLoader = true">Install Modloader</UiButton>
              <UiButton v-else size="small" type="info" :icon="faPen" @click="userSelectModLoader = true">Update Modloader</UiButton>
            </div>
          </div>
        </div>
      </div>
    </template>
  </InstanceSettings>
  
  <div class="pack-settings">
    
    
<!--    <ui-toggle-->
<!--      label="Fullscreen"-->
<!--      desc="Always open Minecraft in Fullscreen mode"-->
<!--      v-model="instanceSettings.fullScreen"-->
<!--      @input="() => {-->
<!--          saveSettings();-->
<!--      }"-->
<!--      class="mb-4"-->
<!--      :align-right="true"-->
<!--    />-->
    
<!--    <div class="mb-6" :class="{'cursor-not-allowed opacity-50 pointer-events-none': instanceSettings.fullScreen}">-->
<!--      <div class="flex items-center mb-4">-->
<!--        <div class="block flex-1 mr-2">-->
<!--          <b>Size presets</b>-->
<!--          <small class="text-muted block mt-2">Select a preset based on your system</small>-->
<!--        </div>-->
<!--        -->
<!--        <selection2-->
<!--          v-if="resolutionList.length"-->
<!--          v-model="resolutionId"-->
<!--          @updated="selectResolution"-->
<!--          :style="{width: '220px'}"-->
<!--          :options="resolutionList"-->
<!--        />-->
<!--      </div>-->
<!--      <div class="flex items-center mb-4">-->
<!--        <div class="block flex-1 mr-2">-->
<!--          <b>Width</b>-->
<!--          <small class="text-muted block mt-2">The Minecraft windows screen width</small>-->
<!--        </div>-->
<!--        <InputNumber class="mb-0" v-model="instanceSettings.width" @blur="saveSettings" @update:model-value="saveSettings()" />-->
<!--      </div>-->
<!--      <div class="flex items-center">-->
<!--        <div class="block flex-1 mr-2">-->
<!--          <b>Height</b>-->
<!--          <small class="text-muted block mt-2">The Minecraft windows screen height</small>-->
<!--        </div>-->
<!--        <InputNumber class="mb-0" v-model="instanceSettings.height" @blur="saveSettings" @update:model-value="saveSettings()" />-->
<!--      </div>-->
<!--    </div>-->
    
    <Modal :open="userSelectModLoader" title="Select Mod Loader" :sub-title="`This instance is currently using ${hasModloader ? instance.modLoader : 'Vanilla'}`" @closed="() => {
      userSelectModLoader = false
      userSelectedLoader = null
    }">
      
      <modloader-select @select="e => userSelectedLoader = e" :mc-version="instance.mcVersion" :provide-latest-option="false" :show-none="false" />

      <div class="text-lg mt-8 mb-4"><FontAwesomeIcon class="mr-2" :icon="faInfoCircle" />Notes</div>
      
      <p class="mb-3">You can select a Mod Loader to install or switch to using the selection below. Please be aware that if you are currently running a modpack, switching Mod Loader version may break the modpack.</p>
      <p>Switching between Mod Loader provider (aka: Forge -> Fabric) will <b>not</b> remove incompatible mods.</p>
      
      <template #footer>
        <div class="flex justify-end gap-4">
          <UiButton type="warning" :icon="faTimes" @click="() => {
            userSelectModLoader = false
            userSelectedLoader = null
          }">Close</UiButton>
          <UiButton type="success" :wider="true" :icon="faDownload" :disabled="userSelectedLoader === null" @click="() => installModLoader()">Install</UiButton>
        </div>
      </template>
    </Modal>
  </div>
</template>
