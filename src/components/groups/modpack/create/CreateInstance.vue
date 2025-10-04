<script lang="ts" setup>
import UiButton from '@/components/ui/UiButton.vue';
import {stringIsEmpty, toTitleCase} from '@/utils/helpers/stringHelpers';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import Loader from '@/components/ui/Loader.vue';
import {ModLoaderWithPackId} from '@/core/types/modpacks/modloaders';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import ModloaderSelect from '@/components/groups/modpack/components/ModloaderSelect.vue';
import UiToggle from '@/components/ui/UiToggle.vue';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {getColorForReleaseType, safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { computed, onMounted, watch, ref } from 'vue';
import {ModalBody, Modal, ModalFooter, Input, UiBadge} from '@/components/ui';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useModpackStore } from '@/store/modpackStore.ts';
import { ModPack } from '@/core/types/appTypes.ts';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { faArrowLeft, faArrowRight, faBoxes, faInfo, faPuzzlePiece } from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';
import ArtworkSelector from "@/components/groups/modpack/components/ArtworkSelector.vue";
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import dayjs, {Dayjs} from "dayjs";
import {defaultInstanceCategory} from "@/core/constants.ts";
import ResolutionSelector from "@/components/groups/modpack/components/ResolutionSelector.vue";

const appStore = useAppStore();
const appSettingsStore = useAppSettings();
const modpackStore = useModpackStore();

const { open } = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const step = ref(0);

const userSelectedArtwork = ref<string | null>(null);
const userPackName = ref("");
const userVanillaVersion = ref("-1");
const userModLoader = ref<[string, ModLoaderWithPackId] | null>(null)
const userCategory = ref(defaultInstanceCategory);

const vanillaPack = ref<ModPack | null>(null);
const showVanillaSnapshots = ref(false);

const loadingVanilla = ref(false);

const fatalError = ref(false);

const userOverrideInstanceDefaults = ref(false);
const userOverridePreferences = ref(false);

const settingRam = ref(0);
const settingResolution = ref({
  width: 0,
  height: 0,
  fullScreen: false
});

function close() {
  step.value = 0;
  userSelectedArtwork.value = null;
  userPackName.value = "";
  userVanillaVersion.value = "-1";
  showVanillaSnapshots.value = false;
  
  userModLoader.value = null;
  userCategory.value = "Default";
  vanillaPack.value = null;
  loadingVanilla.value = false;
  fatalError.value = false;
  userOverrideInstanceDefaults.value = false;
  userOverridePreferences.value = false;
  settingRam.value = 0;
  settingResolution.value = {
    width: 0,
    height: 0,
    fullScreen: false
  };
  emit('close');
}

onMounted(async () => {
  await loadInitialState();
})

watch(() => open, async (newValue) => {
  if (!newValue) {
    return;
  }
  
  await loadInitialState();
})

async function loadInitialState() {
  // Load back in the user defaults
  settingRam.value = appSettingsStore.rootSettings?.instanceDefaults.memory ?? 0;
  settingResolution.value = {
    width: appSettingsStore.rootSettings?.instanceDefaults.width ?? 0,
    height: appSettingsStore.rootSettings?.instanceDefaults.height ?? 0,
    fullScreen: appSettingsStore.rootSettings?.instanceDefaults.fullscreen ?? false
  };
  
  vanillaPack.value = await toggleBeforeAndAfter(() => modpackStore.getModpack(81, "modpacksch") ?? null, state => loadingVanilla.value = state);

  if (!vanillaPack) {
    alertController.error("Failed to load Minecraft versions")
    fatalError.value = true;
    return;
  }

  const vanillaLatest = vanillaVersions.value[0];
  userPackName.value = `Minecraft ${vanillaLatest.value}`;
  userVanillaVersion.value = vanillaLatest.key;
}

watch(userVanillaVersion, (newValue, oldValue) => {
  if (!vanillaPack) {
    return;
  }

  const version = vanillaPack.value?.versions.find(e => e.id.toString() === newValue);
  if (!version) {
    return;
  }

  const lastVersion = vanillaPack.value?.versions.find(e => e.id.toString() === oldValue);
  // Update pack name
  if (userPackName.value === `Minecraft ${lastVersion?.name}`) {
    userPackName.value = `Minecraft ${version.name}`;
  }
})

const canProceed = computed(() => {
  if (fatalError.value) {
    return false;
  }

  switch (step.value) {
    case 0:
      return !stringIsEmpty(userPackName.value) && userVanillaVersion.value !== "-1";
    case 1:
    case 2:
      return true;
    default:
      return false;
  }
});

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
    logo: userSelectedArtwork.value ?? null,
    versionName: "",
    private: false,
    category: userCategory.value,
    ourOwn: true,
    ram: settingRam.value == appSettingsStore.rootSettings?.instanceDefaults.memory ? -1 : settingRam.value,
    fullscreen: settingResolution.value.fullScreen === appSettingsStore.rootSettings?.instanceDefaults.fullscreen ? undefined : settingResolution.value.fullScreen,
    width: settingResolution.value.width == appSettingsStore.rootSettings?.instanceDefaults.width ? undefined : settingResolution.value.width,
    height: settingResolution.value.height == appSettingsStore.rootSettings?.instanceDefaults.height ? undefined : settingResolution.value.height,
  }

  // Magic
  if (!userModLoader.value) {
    appStore.controllers.install.requestInstall({
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

    appStore.controllers.install.requestInstall(request)
  }

  close()
  safeNavigate(RouterNames.ROOT_LIBRARY);
}

const selectedMcVersion = computed(() => {
  if (!vanillaPack || userVanillaVersion.value === "-1") {
    return "unknown";
  }

  return vanillaPack.value?.versions.find(e => e.id.toString() === userVanillaVersion.value)?.name ?? "unknown";
})

const vanillaVersions = computed<UiSelectOption<{ type: string, date: Dayjs }>[]>(() => {
  if (!vanillaPack) {
    return []
  }
  
  return (vanillaPack.value?.versions
    .filter(e => e.type.toLowerCase() === "release" || ((e.type.toLowerCase() === "alpha" || e.type.toLowerCase() === "beta") && showVanillaSnapshots.value))
    .sort((a, b) => b.id - a.id)
    .map(e => ({
      value: e.name,
      key: e.id.toString(),
      type: e.type,
      date: dayjs.unix(e.updated)
    })) ?? []) as UiSelectOption<{ type: string, date: Dayjs }>[];
})
</script>

<template>
  <Modal :open="open" @closed="close" :external-contents="true" title="Create instance" sub-title="Build your own Vanilla or Modded experience">
    <ModalBody>
      <header>
        <div class="steps flex gap-6 pt-4 pb-6">
          <div class="step flex-1" :class="{active: step === 0, done: step > 0}"><FontAwesomeIcon fixedWidth :icon="faInfo" /> About <span /></div>
          <div class="step flex-1" :class="{active: step === 1, done: step > 1}"><FontAwesomeIcon fixedWidth :icon="faBoxes" /> Mod Loader <span /></div>
          <div class="step flex-1" :class="{active: step === 2}"><FontAwesomeIcon fixedWidth :icon="faPuzzlePiece" /> Settings <span /></div>
        </div>
      </header>
      <div class="about" v-show="step === 0">
        <template v-if="!loadingVanilla">
          <ArtworkSelector class="mb-6" v-model="userSelectedArtwork" />
          <Input fill label="Name" placeholder="Next best instance!" v-model="userPackName" class="mb-4" />
          
          <UiSelect :options="vanillaVersions" class="mb-4" label="Minecraft version" v-model="userVanillaVersion" :min-width="320">
            <template #option="{ option, clazz }">
              <div :class="clazz" class="flex justify-between items-center gap-4">
                <div class="flex-1">{{ option.value }}</div>
                <div class="text-white/80">{{ option.date.fromNow() }}</div>
                <UiBadge class="font-bold text-shadow" :style="{backgroundColor: getColorForReleaseType(option.type)}">{{ toTitleCase(option.type) }}</UiBadge>
              </div>
            </template>
          </UiSelect>
          
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
          <ResolutionSelector v-model="settingResolution" />
        </div>
      </div>
    </ModalBody>
    
    <ModalFooter>
      <div class="flex justify-end">
        <UiButton v-if="step > 0" :icon="faArrowLeft" class="mr-2" @click="step --">
          Back
        </UiButton>
        <UiButton :wider="true" type="success" v-if="step < 2" :disabled="!canProceed" :icon="faArrowRight" @click="step ++">
          Next
        </UiButton>
        <UiButton :wider="true" type="success" v-if="step === 2" :icon="faArrowRight" @click="createInstance">
          Create
        </UiButton>
      </div>
    </ModalFooter>
  </Modal>
</template>

<style lang="scss" scoped>
@import 'tailwindcss/theme' theme(reference);

.steps {
  .step {
    display: flex;
    align-items: center;
    gap: .5rem;
    color: rgba(white, .5);
    
    &.active {
      color: var(--color-blue-400);
      
      span {
        background: var(--color-blue-400);
      }
    }
    
    &.done {
      color: var(--color-green-400);
      
      span {
        background: var(--color-green-400);
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