<template>
  <div class="app-settings">
    <div class="app-info-section mb-8">
      <div class="items sm:flex">
        <div class="title-value mr-10">
          <div class="title text-muted mb-1">UI Version</div>
          <div class="value">
            <span class="select-text">{{ webVersion }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <font-awesome-icon
                @click="copyToClipboard(webVersion)"
                class="ml-2 cursor-pointer"
                icon="copy"
                size="1x"
              />
            </div>
            <a class="cursor-pointer hover:underline" href="https://go.ftb.team/app-feedback" target="_blank"
            ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
            /></a>
          </div>
        </div>

        <div class="title-value mt-4 sm:mt-0">
          <div class="title text-muted mb-1">App Version</div>
          <div class="value">
            <span class="select-text">{{ appVersion }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <font-awesome-icon
                @click="copyToClipboard(appVersion)"
                class="ml-2 cursor-pointer"
                icon="copy"
                size="1x"
              />
            </div>
            <a class="cursor-pointer hover:underline" href="https://go.ftb.team/app-feedback" target="_blank"
            ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
            /></a>
          </div>
        </div>
      </div>

      <router-link :to="{ name: 'license' }" class="hover:underline cursor-pointer text-muted text-sm mt-4 block"
      >Software License Information</router-link
      >
    </div>
    
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

<!--    <ui-toggle-->
<!--      label="Enable MineTogether Chat"-->
<!--      desc="Enabled you to use the MineTogether chat from within the app. Currently disabled..."-->
<!--      :value="false"-->
<!--      :disabled="true"-->
<!--      class="mb-8"-->
<!--    />-->

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
    <div class="flex items-center gap-4 mb-6">
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('home')" :working="working">Home</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('instances')" :working="working">Instances</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('logs')" :working="working">Logs</ui-button>
    </div>

    <div class="section logs mb-6 sm:flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Create app logs</p>
        <p class="text-muted pr-10">
          If you're having an issue with the app, you can use this to upload the latest logs from the App. You can the
          provide these logs to our App team to investigate.
        </p>
      </div>
      <ui-button size="small" class="mt-6 sm:mt-0 my-2 w-2/7" type="info" @click="uploadLogData" icon="upload">Upload App Logs</ui-button>
    </div>

    <div class="section cache mb-6 sm:flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Manage cache</p>
        <p class="text-muted pr-10">
          Having an issue with the App loading content? Maybe you want to check if you've got the latest data? Refresh
          the cache, it'll make sure you're running the latest version of all available data.
        </p>
      </div>
      <ui-button size="small" class="mt-6 sm:mt-0 my-2 w-2/7" type="info" @click="refreshCachePlz" icon="sync">Refresh Cache</ui-button>
    </div>

    <ui-toggle
      label="Verbose"
      desc="Enabled very detailed logging for the FTB App... You likely don't need this but it could be helpful?"
      :value="localSettings.verbose"
      @input="enableVerbose"
      :align-right="true"
    />
    
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
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {InstanceActions} from '@/core/actions/instanceActions';

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

  webVersion: string = platform.get.config.webVersion;
  appVersion: string = platform.get.config.appVersion;
  
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

  public async uploadLogData(): Promise<void> {
    const result = await sendMessage("uploadLogs", {
      uiVersion: this.webVersion
    })

    if (!result.error) {
      const url = `https://pste.ch/${result.code}`;
      platform.get.cb.copy(url);
      alertController.success('The URL has been copied to your clipboard')
    }
  }

  public refreshCachePlz() {
    InstanceActions.clearInstanceCache()
  }

  public enableVerbose(value: boolean): void {
    this.localSettings.verbose = value;
    this.saveSettings(this.localSettings);
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
