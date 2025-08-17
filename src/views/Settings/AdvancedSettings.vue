<script lang="ts" setup>
import appPlatform from '@platform';
import UiToggle from '@/components/ui/UiToggle.vue';
import {SettingsData} from '@/core/types/javaApi';
import Loader from '@/components/ui/Loader.vue';
import { onMounted, ref } from 'vue';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { Message } from '@/components/ui';
import { faWarning } from '@fortawesome/free-solid-svg-icons';
import ReleaseChannelSelector from "@/components/groups/modpack/components/ReleaseChannelSelector.vue";

type ReleaseChannel = 'release' | 'beta' | 'alpha';

const appSettingsStore = useAppSettings();
const loading = ref(false);

const appChannel = ref<ReleaseChannel>("release");
const verboseMode = ref(false);
const ignoreForgeProcessorOutputHashes = ref(false);

const checkingForUpdates = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([
      new Promise(async (res) => {
        const channel = await getChannel();
        if (channel === "latest") {
          appChannel.value = "release";
        } else {
          appChannel.value = (channel as ReleaseChannel) ?? "release";
        }

        res(null);
      }),

      await appSettingsStore.loadSettings()
    ])
    
    console.log(JSON.parse(JSON.stringify(appSettingsStore.rootSettings)))
  } catch (e) {
    console.error('Failed to load settings', e);
  } finally {
    loading.value = false;
  }
  
  verboseMode.value = appSettingsStore.rootSettings?.general.verbose ?? false;
  ignoreForgeProcessorOutputHashes.value = appSettingsStore.rootSettings?.workaround.ignoreForgeProcessorOutputHashes ?? false;
})

function enableVerbose(): void {
  const newSettings = {
    ...appSettingsStore.rootSettings,
    general: {
      ...appSettingsStore.rootSettings?.general,
      verbose: verboseMode.value
    }
  } as SettingsData
  
  appSettingsStore.saveSettings(newSettings);
}

function enableForgeProcessorHashes(): void {
  const newSettings = {
    ...appSettingsStore.rootSettings,
    workaround: {
      ...appSettingsStore.rootSettings?.workaround,
      ignoreForgeProcessorOutputHashes: ignoreForgeProcessorOutputHashes.value
    }
  } as SettingsData
  
  appSettingsStore.saveSettings(newSettings);
}

async function getChannel() {
  try {
    return await appPlatform.app.appChannel();
  } catch (e) {
    console.error('Failed to get app channel', e);
    return "release";
  }
}

async function setAppChannel() {
  if (!appChannel.value) {
    return;
  }
  
  try {
    await toggleBeforeAndAfter(async () => appPlatform.app.changeAppChannel(transformToElectronChannel(appChannel.value)), v => checkingForUpdates.value = v);
  } catch (e) {
    console.error('Failed to set app channel', e);
  }
}

function transformToElectronChannel(channel: ReleaseChannel) {
  switch (channel) {
    case 'release': return 'latest';
    case 'beta': return 'beta';
    case 'alpha': return 'alpha';
    default: return 'latest';
  }
}
</script>

<template>
  <div class="app-settings" v-if="!loading">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-4">Advanced</h1>
      
      <message header="Warning" type="warning" class="mb-4" :icon="faWarning">
        These settings are for advanced users only, changing these settings could have unintended consequences.
      </message>
    </div>

    <div class="mb-4 border-b border-white/10 pb-6" v-if="appPlatform.isElectron">
      <p class="block text-white-700 text-lg font-bold mb-4">Updates</p>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Release Channel</b>
          <small class="block text-xs text-muted mr-6 mt-2">
            Feeling adventurous? Switch to the beta or alpha channel to get the latest features before they're released to everyone.
          </small>
        </div>
        
        <ReleaseChannelSelector v-model="appChannel" @update:modelValue="setAppChannel" :disabled="checkingForUpdates" />
      </div>
      
      <p v-if="checkingForUpdates">Checking for updates...</p>
    </div>
    
    <div class="mb-4 border-b border-white/10 pb-6">
      <p class="block text-white-700 text-lg font-bold mt-6 mb-2">Troubleshooting</p>
      <p class="text-muted text-sm mb-4">These settings are for troubleshooting purposes only</p>
      
      <ui-toggle
        label="Verbose"
        desc="Enabled very detailed logging for the FTB App... You likely don't need this but it could be helpful?"
        v-model="verboseMode"
        @input="enableVerbose"
      />
    </div>

    <p class="block text-white-700 text-lg font-bold mt-6 mb-2">Workarounds</p>
    <p class="text-muted text-sm mb-4">Every now and then, very specific issues can be resolved by enabling a workaround. These are not recommended for general use.</p>

    <ui-toggle
      label="Ignore (Neo)Forge processor hashes checks"
      desc="This workaround will ignore the processor hashes checks for Forge and NeoForge. This is currently only a known issue with Fedora Linux using a specific fork of zlib (zlib-ng)."
      v-model="ignoreForgeProcessorOutputHashes"
      @input="enableForgeProcessorHashes"
    />
  </div>
  <Loader v-else />
</template>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}
</style>
