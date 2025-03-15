<template>
  <div class="app-settings" v-if="localSettings.spec">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-2">Advanced</h1>
      <p class="text-muted flex items-center gap-2">
        <span class="bg-warning rounded text-white font-bold px-2 py-0.5">Warning</span> 
        These settings are for advanced users only, changing these settings could have unintended consequences.
      </p>
    </div>

    <div class="mb-4 border-b border-white border-opacity-25 pb-6" v-if="platform.isElectron()">
      <p class="block text-white-700 text-lg font-bold mb-4">Updates</p>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Release Channel</b>
          <small class="block text-muted mr-6 mt-2">
            Feeling adventurous? Switch to the beta or alpha channel to get the latest features before they're released to everyone.
          </small>
        </div>
  
        <selection2
          :disabled="checkingForUpdates"
          :options="channelOptions"
          v-model="appChannel"
          :style="{width: '192px'}"
          @change="setAppChannel"
        />
      </div>
      
      <p v-if="checkingForUpdates">Checking for updates...</p>
    </div>
    
    <div class="mb-4 border-b border-white border-opacity-25 pb-6">
      <p class="block text-white-700 text-lg font-bold mt-6 mb-2">Troubleshooting</p>
      <p class="text-muted text-sm mb-4">These settings are for troubleshooting purposes only</p>
      
      <ui-toggle
        label="Verbose"
        desc="Enabled very detailed logging for the FTB App... You likely don't need this but it could be helpful?"
        :value="localSettings.general.verbose"
        @input="enableVerbose"
      />
    </div>

    <p class="block text-white-700 text-lg font-bold mt-6 mb-2">Workarounds</p>
    <p class="text-muted text-sm mb-4">Every now and then, very specific issues can be resolved by enabling a workaround. These are not recommended for general use.</p>

    <ui-toggle
      :key="fixMeKey"
      label="Ignore (Neo)Forge processor hashes checks"
      desc="This workaround will ignore the processor hashes checks for Forge and NeoForge. This is currently only a known issue with Fedora Linux using a specific fork of zlib (zlib-ng)."
      :value="localSettings.workaround.ignoreForgeProcessorOutputHashes"
      @input="enableForgeProcessorHashes"
    />
  </div>
  <Loader v-else />
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {SettingsState} from '@/modules/settings/types';
import {Action, State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import UiToggle from '@/components/ui/UiToggle.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {SettingsData} from '@/core/@types/javaApi';
import Loader from '@/components/ui/Loader.vue';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import Selection2 from '@/components/ui/Selection2.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';

type ReleaseChannel = 'release' | 'beta' | 'alpha';

@Component({
  components: {
    Selection2,
    ProgressBar,
    Loader,
    UiButton,
    UiToggle,
  },
})
export default class AppSettings extends Vue {
  @State('settings') settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) loadSettings: any;

  platform = platform;
  localSettings: SettingsData = {} as SettingsData;
  
  fixMeKey = Math.random();
  
  checkingForUpdates = false;
  appChannel: ReleaseChannel = "release";
  
  async created() {
    await Promise.all([
      new Promise(async (res) => {
        const channel = await this.getChannel();
        if (channel === "latest") {
          this.appChannel = "release";
        } else {
          this.appChannel = (channel as ReleaseChannel) ?? "release";
        }

        res(null);
      }),
      this.loadSettings()
    ])

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
    
    // Fix for when the setting does not exist in the initial state
    if (!this.localSettings.workaround) {
      this.localSettings.workaround = {
        ignoreForgeProcessorOutputHashes: false,
      }
    }
  }

  public enableVerbose(value: boolean): void {
    this.localSettings.general.verbose = value;
    this.saveSettings(this.localSettings);
  }
  
  public enableForgeProcessorHashes(value: boolean): void {    
    this.localSettings.workaround.ignoreForgeProcessorOutputHashes = value;
    this.saveSettings(this.localSettings);
    this.fixMeKey = Math.random();
  }
  
  get channelOptions() {
    return ReleaseChannelOptions(false)
  }
  
  public async getChannel() {
    console.log(await platform.get.app.appChannel());
    return await platform.get.app.appChannel();
  }
  
  public async setAppChannel() {
    if (!this.appChannel) {
      return;
    }
    
    this.checkingForUpdates = true;
    try {
      await platform.get.app.changeAppChannel(this.transformToElectronChannel(this.appChannel));
    } catch (e) {
      console.error('Failed to set app channel', e);
    }
    
    this.checkingForUpdates = false;
  }
  
  transformToElectronChannel(channel: ReleaseChannel) {
    switch (channel) {
      case 'release': return 'latest';
      case 'beta': return 'beta';
      case 'alpha': return 'alpha';
      default: return 'latest';
    }
  }
}
</script>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}
</style>
