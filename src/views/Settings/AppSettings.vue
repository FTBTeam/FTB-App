<template>
  <div class="app-settings">
    <!-- <ftb-toggle label="Enable Analytics: " :value="settingsCopy.enableAnalytics" @change="enableAnalytics"
                    onColor="bg-primary"/> -->
<!--    <ui-toggle-->
<!--      label="Use the beta channel"-->
<!--      desc="This allows you to opt-in to the beta channel of the FTB App, this version is typically less stable than the normal release channel."-->
<!--      :value="localSettings.enablePreview"-->
<!--      @input="enablePreview"-->
<!--      class="mb-8"-->
<!--    />-->
<!--    -->
    <ui-toggle
      v-if="!platform.isElectron()"
      label="Close Overwolf on Exit"
      desc="If enabled, we'll automatically close the Overwolf app when you exit the FTB App, can be useful if you don't use Overwolf."
      :value="localSettings.exitOverwolf"
      @input="exitOverwolf"
      class="mb-8"
    />

    <ui-toggle
      label="Enable MineTogether Chat"
      desc="Enabled you to use the MineTogether chat from within the app. Currently disabled..."
      :value="false"
      :disabled="true"
      class="mb-8"
    />

    <p class="block text-white-700 text-lg font-bold mb-4" v-if="platform.isElectron()">Appearance</p>

    <ui-toggle
      v-if="platform.isElectron()"
      label="Use systems window style"
      desc="Instead of using the apps internal Titlebar, we'll use the system's titlebar instead. This setting will restart the app!"
      v-model="localSettings.useSystemWindowStyle"
      @input="toggleSystemStyleWindow"
      class="mb-8"
    />


    <p class="block text-white-700 text-lg font-bold mb-4">Actions</p>

    <p class="block text-white-700 font-bold mb-4">Open paths</p>
    <div class="flex items-center gap-4">
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('home')" :working="working">Home</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('instances')" :working="working">Instances</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('logs')" :working="working">Logs</ui-button>
    </div>
    
    <!--      :disabled="auth.token === null ? 'true' : ''"-->
    <!--      :value="localSettings.enableChat"-->
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Settings, SettingsState} from '@/modules/settings/types';
import {Action, State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import {AuthState} from '@/modules/auth/types';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import os from 'os';
import path from 'path';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';

@Component({
  components: {
    UiButton,
    UiToggle,
  },
})
export default class AppSettings extends Vue {
  @State('auth') auth!: AuthState;
  @State('settings') settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) loadSettings: any;

  platform = platform;
  localSettings: Settings = {} as Settings;
  
  working = false;

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  enablePreview(value: boolean): void {
    this.localSettings.enablePreview = value;
    this.saveSettings(this.localSettings);
  }

  enableChat(value: boolean): void {
    if (this.auth.token !== null) {
      this.localSettings.enableChat = value;
      this.saveSettings(this.localSettings);
    }
  }

  exitOverwolf(value: boolean): void {
    this.localSettings.exitOverwolf = value;
    this.saveSettings(this.localSettings);
    platform.get.actions.changeExitOverwolfSetting(value);
  }
  
  toggleSystemStyleWindow(value: boolean): void {
    this.localSettings.useSystemWindowStyle = value;
    this.saveSettings(this.localSettings);
    
    platform.get.frame.setSystemWindowStyle(value);
  }
  
  openFolder(location: string) {
    switch (location) {
      case 'home':
        toggleBeforeAndAfter(() => platform.get.io.openFinder(AppSettings.getAppHome()), state => this.working = state)
        break;
      case 'instances': 
        toggleBeforeAndAfter(() => platform.get.io.openFinder(this.localSettings.instanceLocation), state => this.working = state)
        break;
      case 'logs':
        toggleBeforeAndAfter(() => platform.get.io.openFinder(path.join(AppSettings.getAppHome(), 'logs')), state => this.working = state)
        break;
      default:
        toggleBeforeAndAfter(() => platform.get.io.openFinder(location), state => this.working = state)
        break;
    }
  }

  public static getAppHome() {
    if (platform.isOverwolf()) {
      return path.join(platform.get.io.getLocalAppData(), '.ftba');
    } else if (os.platform() === "darwin") {
      return path.join(os.homedir(), 'Library', 'Application Support', '.ftba');
    } else {
      return path.join(os.homedir(), '.ftba');
    }
  }
}
</script>

<style scoped lang="scss"></style>
