<script lang="ts" setup>
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import Selection2, {SelectionOption} from '@/components/ui/Selection2.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import Loader from '@/components/ui/Loader.vue';
import {ModLoaderWithPackId} from '@/core/types/modpacks/modloaders';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import ModloaderSelect from '@/components/groups/modpack/components/ModloaderSelect.vue';
import UiToggle from '@/components/ui/UiToggle.vue';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { computed, onMounted, watch, ref } from 'vue';
import { ModalBody, Modal, FTBInput, ModalFooter } from '@/components/ui';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { services } from '@/bootstrap.ts';
import { useModpackStore } from '@/store/modpackStore.ts';
import { ModPack } from '@/core/types/appTypes.ts';
import { useAppSettings } from '@/store/appSettingsStore.ts';

const appSettingsStore = useAppSettings();
const modpackStore = useModpackStore();

const { open } = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const step = ref(0);

const userSelectedArtwork = ref<File | null>(null);
const userPackName = ref("");
const userVanillaVersion = ref(-1);
const userModLoader = ref<[string, ModLoaderWithPackId] | null>(null)
const userCategory = ref("Default");

const vanillaPack = ref<ModPack | null>(null);
const showVanillaSnapshots = ref(false);

const loadingVanilla = ref(false);

const fatalError = ref(false);

const userOverrideInstanceDefaults = ref(false);
const userOverridePreferences = ref(false);

const settingFullscreen = ref(false);
const settingScreenResolution = ref("");
const settingRam = ref(0);
const userHeight = ref(0);
const userWidth = ref(0);

function close() {
  step.value = 0;
  userSelectedArtwork.value = null;
  userPackName.value = "";
  userVanillaVersion.value = -1;
  showVanillaSnapshots.value = false;
  emit('close');
}

onMounted(async () => {
  await loadInitialState();
  
  settingFullscreen.value = appSettingsStore.rootSettings?.instanceDefaults.fullscreen ?? false;
  settingScreenResolution.value = (appSettingsStore.rootSettings?.instanceDefaults.width ?? 0) + "x" + (appSettingsStore.rootSettings?.instanceDefaults.height ?? 0);
  settingRam.value = appSettingsStore.rootSettings?.instanceDefaults.memory ?? 0;
  userWidth.value = appSettingsStore.rootSettings?.instanceDefaults.width ?? 0;
  userHeight.value = appSettingsStore.rootSettings?.instanceDefaults.height ?? 0;
})

function onScreenResolutionChange(newVal: string) {
  const [width, height] = newVal.split("x");
  userWidth.value = parseInt(width);
  userHeight.value = parseInt(height);
}

watch(() => open, async (newValue) => {
  if (!newValue) {
    return;
  }
  
  await loadInitialState();
})

async function loadInitialState() {
  vanillaPack.value = await toggleBeforeAndAfter(() => modpackStore.getModpack(81, "modpacksch") ?? null, state => loadingVanilla.value = state);

  if (!vanillaPack) {
    alertController.error("Failed to load Minecraft versions")
    fatalError.value = true;
    return;
  }

  const vanillaLatest = vanillaVersions.value[0];
  userPackName.value = `Minecraft ${vanillaLatest.label}`;
  userVanillaVersion.value = vanillaLatest.value;
}

watch(userVanillaVersion, (newValue) => {
  if (!vanillaPack) {
    return;
  }

  const version = vanillaPack.value?.versions.find(e => e.id === newValue);
  if (!version) {
    return;
  }

  const lastVersion = vanillaPack.value?.versions.find(e => e.id === newValue);
  // Update pack name
  if (userPackName.value === `Minecraft ${lastVersion?.name}`) {
    userPackName.value = `Minecraft ${version.name}`;
  }
})

function canProceed() {
  if (fatalError) {
    return false;
  }

  switch (step.value) {
    case 0:
      return !stringIsEmpty(userPackName.value) && userVanillaVersion.value !== -1;
    case 1:
    case 2:
      return true;
    default:
      return false;
  }
}

/**
 * Due to the design of the api and the subprocess, we kinda need to build a special request for each type of
 * pack that is configurable here.
 *
 * Vanilla with no mod loader? Just use the vanilla packid and versionid
 * Modloader? Use the modloader packid and versionid but this is filtered based on the vanilla pack
 */
function createInstance() {
  const sharedData = {
    name: userPackName.value,
    logo: userSelectedArtwork.value?.path ?? "",
    versionName: "",
    private: false,
    category: userCategory.value,
    ourOwn: true,
    ram: settingRam.value == appSettingsStore.rootSettings?.instanceDefaults.memory ? -1 : settingRam.value,
    fullscreen: settingFullscreen.value === appSettingsStore.rootSettings?.instanceDefaults.fullscreen ? undefined : settingFullscreen.value,
    width: userWidth.value == appSettingsStore.rootSettings?.instanceDefaults.width ? undefined : userWidth.value,
    height: userHeight.value == appSettingsStore.rootSettings?.instanceDefaults.height ? undefined : userHeight.value,
  }

  // Magic
  if (!userModLoader.value) {
    services.instanceInstallController.requestInstall({
      id: 81, // Vanilla pack id
      version: userVanillaVersion.value ?? 0,
      ...sharedData
    })
  } else {
    // We're working with a modloader
    const request = {
      id: userModLoader.value[1].packId,
      version: userModLoader.value[1].id,
      ...sharedData,
    } as any;

    if (userModLoader.value[0] === "fabric") {
      request["mcVersion"] = selectedMcVersion;
    }

    services.instanceInstallController.requestInstall(request)
  }

  emit('close')
  safeNavigate(RouterNames.ROOT_LIBRARY);
}

const selectedMcVersion = computed(() => {
  if (!vanillaPack || userVanillaVersion.value === -1) {
    return "unknown";
  }

  return vanillaPack.value?.versions.find(e => e.id === userVanillaVersion.value)?.name ?? "unknown";
})

const vanillaVersions = computed<SelectionOption[]>(() => {
  if (!vanillaPack) {
    return []
  }
  
  return vanillaPack.value?.versions
    .filter(e => e.type.toLowerCase() === "release" || ((e.type.toLowerCase() === "alpha" || e.type.toLowerCase() === "beta") && showVanillaSnapshots))
    .sort((a, b) => b.id - a.id)
    .map(e => ({
      label: e.name,
      value: e.id,
      meta: e.type
    })) as SelectionOption[] ?? []
})

const screenResolutions = computed( () => {
  return appSettingsStore.systemHardware?.supportedResolutions.map(e => ({
    label: `${e.width}x${e.height}`,
    value: `${e.width}x${e.height}`
  })) ?? []
})
</script>

<template>
  <Modal :open="open" @closed="close" :external-contents="true" title="Create instance" sub-title="Build your own Vanilla or Modded experience">
    <ModalBody>
      <header>
        <div class="steps flex gap-6 pt-4 pb-6">
          <div class="step flex-1" :class="{active: step === 0, done: step > 0}"><FontAwesomeIcon fixedWidth icon="info" /> About <span /></div>
          <div class="step flex-1" :class="{active: step === 1, done: step > 1}"><FontAwesomeIcon fixedWidth icon="boxes" /> Mod Loader <span /></div>
          <div class="step flex-1" :class="{active: step === 2}"><FontAwesomeIcon fixedWidth icon="puzzle-piece" /> Settings <span /></div>
        </div>
      </header>
      <div class="about" v-show="step === 0">
        <template v-if="!loadingVanilla">
          <ArtworkSelector class="mb-6" v-model="userSelectedArtwork" />
          <FTBInput label="Name" placeholder="Next best instance!" v-model="userPackName" class="mb-4" />
          <Selection2 :open-up="true" label="Minecraft version" class="mb-4" :options="vanillaVersions" v-model="userVanillaVersion" />
          
          <UiToggle class="mb-4" label="Show snapshots" desc="Snapshot versions of Minecraft are typically unstable and no longer maintained" v-model="showVanillaSnapshots" />
          
          <CategorySelector v-model="userCategory" />
        </template>
        <Loader v-else />
      </div>
      
      <div class="modloader" v-show="step === 1">
        <ModloaderSelect :mc-version="selectedMcVersion" @select="(v) => userModLoader = v ?? null" :show-optional="true" />
      </div>
      
      <div class="settings" v-show="step === 2">
        <div class="mb-4 header flex items-center justify-between">
          <h3 class="font-bold">Instance settings</h3>
          <UiToggle label="Override defaults" class="ui-toggle" :align-right="true" v-model="userOverrideInstanceDefaults" />  
        </div>
        
        <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': !userOverrideInstanceDefaults}">
          <RamSlider v-model="settingRam" class="mb-4" />
        </div>

        <hr />
        
        <div class="mb-4 header flex items-center justify-between">
          <h3 class=" font-bold">Preferences</h3>
          <UiToggle label="Override defaults" class="ui-toggle" :align-right="true" v-model="userOverridePreferences" />
        </div>
        
        <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': !userOverridePreferences}">
          <UiToggle label="Fullscreen" class="mb-4" desc="Set Minecraft to fullscreen the game when possible" v-model="settingFullscreen" />
          
          <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': settingFullscreen}">
            <Selection2 class="mb-4" :open-up="true" :options="screenResolutions" label="Screen resolution" v-model="settingScreenResolution" @input="onScreenResolutionChange" />

            <div class="flex items-center mb-4">
              <div class="block flex-1 mr-2">
                <b>Width</b>
                <small class="text-muted block mt-2">The Minecraft windows screen width</small>
              </div>
              <FTBInput class="mb-0" v-model="userWidth" />
            </div>
            <div class="flex items-center">
              <div class="block flex-1 mr-2">
                <b>Height</b>
                <small class="text-muted block mt-2">The Minecraft windows screen height</small>
              </div>
              <FTBInput class="mb-0" v-model="userHeight" />
            </div>
          </div>
        </div>
        <hr />
        
      </div>
    </ModalBody>
    
    <ModalFooter>
      <div class="flex justify-end">
        <UiButton v-if="step > 0" icon="arrow-left" class="mr-2" @click="step --">
          Back
        </UiButton>
        <UiButton :wider="true" type="success" v-if="step < 2" :disabled="!canProceed()" icon="arrow-right" @click="step ++">
          Next
        </UiButton>
        <UiButton :wider="true" type="success" v-if="step === 2" icon="arrow-right" @click="createInstance">
          Create
        </UiButton>
      </div>
    </ModalFooter>
  </Modal>
</template>

<style lang="scss" scoped>
.steps {
  .step {
    display: flex;
    align-items: center;
    gap: .5rem;
    color: rgba(white, .5);
    
    &.active {
      color: var(--color-light-info-button);
      
      span {
        @apply bg-pink-400;
        background: var(--color-info-button);
      }
    }
    
    &.done {
      color: var(--color-light-success-button);
      
      span {
        background: var(--color-success-button);
      }
    }
    
    span {
      flex: 1;
      height: 2px;
      background: rgba(white, .2);
      border-radius: 2px;
    }
  }
}

hr {
  border: 0;
  border-bottom: 1px solid rgba(white, .1);
  margin: 1rem 0;
}

.header {
  .ui-toggle {
    transform-origin: right center;
    transform: scale(.8);
  }
}
</style>