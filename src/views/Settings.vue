<template>
  <div class="settings">
    <settings-sidebar />
    <main>
      <router-view />
      <div class="flex flex-col md:w-full lg:w-9/12 xl:w-8/12 mx-auto">
        <h1 class="text-2xl">Game Settings</h1>
        <div class="bg-sidebar-item p-5 rounded my-4">
          <div class="flex flex-col my-2">
            <div class="flex flex-col my-2">
              <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2" for="grid-last-name">
                Window Size
              </label>
              <div class="flex flex-row my-4 -mt-2">
                <div class="flex-col mt-auto mb-2 pr-1">
                  <v-selectmenu
                    :title="false"
                    :query="false"
                    :data="resolutionList"
                    align="left"
                    :value="resSelectedValue"
                    @values="resChange"
                  >
                    <ftb-button color="primary" class="py-2 px-4 my-1"
                      ><font-awesome-icon icon="desktop" size="1x" class="cursor-pointer"
                    /></ftb-button>
                    <!--                                    <button class="appearance-none block w-full bg-green-400 text-white-600 border border-green-400 py-3 px-4 leading-tight cursor-pointer">-->
                    <!--                                        <font-awesome-icon icon="desktop" size="1x" class="cursor-pointer"/>-->
                    <!--                                    </button>-->
                  </v-selectmenu>
                </div>

                <ftb-input
                  class="flex-col"
                  label="Width"
                  v-model="settingsCopy.width"
                  :value="settingsCopy.width"
                  @blur="doSave"
                />
                <font-awesome-icon class="mt-auto mb-6 mx-1 text-gray-600" icon="times" size="1x" />
                <ftb-input
                  class="flex-col"
                  label="Height"
                  v-model="settingsCopy.height"
                  :value="settingsCopy.height"
                  @blur="doSave"
                />
              </div>
            </div>
            <ftb-toggle
              label="Keep launcher open when game starts: "
              :value="settingsCopy.keepLauncherOpen"
              @change="keepLauncherOpen"
              onColor="bg-primary"
            />
            <!--          <ftb-slider label="Default Memory" v-model="settingsCopy.memory" :currentValue="settingsCopy.memory" minValue="512" :maxValue="settingsState.hardware.totalMemory" @change="doSave"-->
            <!--                      unit="MB" @blur="doSave" step="128"/>-->

            <ftb-input
              label="Custom Arguments"
              :value="settingsCopy.jvmargs"
              v-model="settingsCopy.jvmargs"
              @blur="doSave"
            />
            <ftb-input
              label="Instance Location"
              :value="settingsCopy.instanceLocation"
              v-model="settingsCopy.instanceLocation"
              @blur="doSave"
              button="true"
              buttonText="Browse"
              buttonColor="primary"
              :buttonClick="browseForFolder"
            />
          </div>
        </div>
        <h1 class="text-2xl">App Settings</h1>
        <div class="bg-sidebar-item p-5 rounded my-4">
          <div class="flex flex-col my-2">
            <!-- <ftb-toggle label="Enable Analytics: " :value="settingsCopy.enableAnalytics" @change="enableAnalytics"
                                onColor="bg-primary"/> -->
            <ftb-toggle
              v-if="platform.isElectron()"
              label="Enable Preview Versions: "
              :value="settingsCopy.enablePreview"
              @change="enablePreview"
              onColor="bg-primary"
            />
            <ftb-toggle
              v-else
              label="Close Overwolf on Exit: "
              :value="settingsCopy.exitOverwolf"
              @change="exitOverwolf"
              onColor="bg-primary"
            />
            <ftb-toggle
              label="Enable MineTogether Chat: "
              :value="settingsCopy.enableChat"
              @change="enableChat"
              onColor="bg-primary"
              :disabled="auth.token === null ? 'true' : ''"
            />
            <ftb-toggle
              label="Automate Minecraft Launcher: "
              :value="settingsCopy.automateMojang"
              @change="enableAutomate"
              onColor="bg-primary"
            />
            <ftb-slider
              label="Download Threads"
              v-model="settingsCopy.threadLimit"
              :currentValue="settingsCopy.threadLimit"
              minValue="1"
              :maxValue="settingsState.hardware.totalCores * 2"
              @change="doSave"
              unit="threads"
              @blur="doSave"
            />
            <ftb-slider
              label="Download Speed"
              v-model="settingsCopy.speedLimit"
              :currentValue="(settingsCopy.speedLimit / 1000).toFixed(0)"
              minValue="0"
              :maxValue="250000"
              @change="doSave"
              step="256"
              unit="mbps"
              maxValueLabel="250"
              @blur="doSave"
            />
            <ftb-slider
              label="Cache Life"
              v-model="settingsCopy.cacheLife"
              :currentValue="settingsCopy.cacheLife"
              minValue="900"
              maxValue="15768000"
              @change="doSave"
              unit="s"
              maxValueLabel="15768000"
              @blur="doSave"
              description="How long data will live before it dies"
            />
          </div>
        </div>

        <h1 class="text-2xl">App Info</h1>
        <div class="bg-sidebar-item p-5 rounded my-4">
          <div class="flex flex-col my-2">
            <span>
              UI Version: {{ webVersion }}
              <font-awesome-icon
                @click="copyToClipboard(webVersion)"
                class="ml-2 cursor-pointer"
                :icon="['fas', 'copy']"
                size="1x"
              />
              <a
                class="cursor-pointer hover:underline"
                :href="
                  `${
                    platform.isElectron()
                      ? 'https://github.com/FTBTeam/FTB-App'
                      : 'https://github.com/FTBTeam/FTB-App-Overwolf'
                  }`
                "
                target="_blank"
                ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
              /></a>
            </span>
            <span
              >App Version: {{ appVersion }}
              <font-awesome-icon
                @click="copyToClipboard(appVersion)"
                class="ml-2 cursor-pointer"
                :icon="['fas', 'copy']"
                size="1x"/><a
                class="cursor-pointer hover:underline"
                href="https://github.com/CreeperHost/modpacklauncher"
                target="_blank"
                ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"/></a
            ></span>
            <router-link @click.native="scrollToTop" to="/license" class="hover:underline cursor-pointer"
              >License Information</router-link
            >
            <ftb-button class="py-2 px-4 my-2" color="primary" css-class="text-center text-l" @click="uploadLogData()"
              >Upload App Logs</ftb-button
            >
            <ftb-button class="py-2 px-4 my-2" color="warning" css-class="text-center text-l" @click="refreshCache()"
              >Refresh Cache</ftb-button
            >
            <ftb-toggle label="Verbose" :value="settingsCopy.verbose" @change="enableVerbose" onColor="bg-primary" />
            <!--                    <button @click="uploadLogData()"-->
            <!--                            class="appearance-none block w-full bg-green-400 text-white-600 border border-green-400 my-2 py-3 px-4 leading-tight cursor-pointer">-->
            <!--                        Upload App Logs-->
            <!--                    </button>-->
            <!--                    <button @click="refreshCache()"-->
            <!--                            class="appearance-none block w-full bg-orange-400 text-white-600 border border-orange-500 my-2 py-3 px-4 leading-tight cursor-pointer">-->
            <!--                       Refresh Cache-->
            <!--                    </button>-->
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import { State, Action } from 'vuex-class';
import { SettingsState, Settings } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';
import platform from '@/utils/interface/electron-overwolf';
import SettingsSidebar from '@/components/SettingsSidebar.vue';

@Component({
  components: {
    'ftb-input': FTBInput,
    'ftb-toggle': FTBToggle,
    'ftb-slider': FTBSlider,
    'ftb-button': FTBButton,
    PackCardWrapper,
    SettingsSidebar,
  },
})
export default class SettingsPage extends Vue {
  platform = platform;
  @State('auth') private auth!: AuthState;
  @State('settings') public settingsState!: SettingsState;
  @Action('refreshCache', { namespace: 'modpacks' }) public refreshCache!: any;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('showAlert') public showAlert: any;
  @Action('hideAlert') public hideAlert: any;
  @Action('sendMessage') public sendMessage: any;

  public settingsCopy: Settings = {
    width: 1720,
    height: 840,
    memory: 3072,
    keepLauncherOpen: true,
    enablePreview: false,
    jvmargs: '',
    enableAnalytics: true,
    enableChat: true,
    enableBeta: false,
    exitOverwolf: false, // Clearly only for overwolf
    threadLimit: 2,
    speedLimit: 0,
    cacheLife: 5184000,
    packCardSize: 1,
    instanceLocation: '',
    listMode: false,
    verbose: false,
    cloudSaves: false,
    autoOpenChat: true,
    blockedUsers: [],
    mtConnect: false,
    automateMojang: true,
    showAdverts: true,
    loadInApp: true,
  };

  private resSelectedValue: string = '0';
  private webVersion: string = platform.get.config.webVersion;
  private appVersion: string = platform.get.config.appVersion;

  public scrollToTop(): void {
    // TODO: this likely isn't needed due to how rendering works and the changes made to the license page
    document.querySelectorAll('.app-content')[0].scrollTo(0, 0);
  }

  public browseForFolder() {
    platform.get.io.selectFolderDialog(this.settingsCopy.instanceLocation, path => {
      this.settingsCopy.instanceLocation = path;
      this.saveSettings(this.settingsCopy);
    });
  }

  public async uploadLogData(): Promise<void> {
    this.sendMessage({
      payload: { type: 'uploadLogs', uiVersion: this.webVersion },
      callback: async (data: any) => {
        if (!data.error) {
          const url = `https://pste.ch/${data.code}`;
          platform.get.cb.copy(url);
          this.showAlert({
            title: 'Uploaded!',
            message: 'The URL has been copied to your clipboard',
            type: 'primary',
          });
          setTimeout(() => {
            this.hideAlert();
          }, 5000);
        }
      },
    });
  }

  public resChange(data: any) {
    if (data && data.length) {
      if (this.resSelectedValue === data[0].value) {
        return;
      }
      this.resSelectedValue = data[0].value;
      this.settingsCopy.width = this.settingsState.hardware.supportedResolutions[data[0].id].width;
      this.settingsCopy.height = this.settingsState.hardware.supportedResolutions[data[0].id].height;

      this.doSave();
      return;
    }
  }

  public async created() {
    await this.loadSettings();
    this.settingsCopy = { ...this.settingsCopy, ...this.settingsState.settings };
    Object.keys(this.settingsCopy).forEach((key: string) => {
      if (key === 'listMode' && this.settingsCopy[key] === undefined) {
        this.settingsCopy[key] = false;
      }
      if ((this.settingsCopy as any)[key] === 'true') {
        (this.settingsCopy as any)[key] = true;
      } else if ((this.settingsCopy as any)[key] === 'false') {
        (this.settingsCopy as any)[key] = false;
      } else if (key !== 'jvmargs' && key !== 'instanceLocation' && !isNaN((this.settingsCopy as any)[key])) {
        (this.settingsCopy as any)[key] = parseInt((this.settingsCopy as any)[key], 10);
      }
    });
  }

  public keepLauncherOpen(value: boolean): void {
    this.settingsCopy.keepLauncherOpen = value;
    this.saveSettings(this.settingsCopy);
  }

  public enablePreview(value: boolean): void {
    this.settingsCopy.enablePreview = value;
    this.saveSettings(this.settingsCopy);
  }

  public enableChat(value: boolean): void {
    if (this.auth.token !== null) {
      this.settingsCopy.enableChat = value;
      this.saveSettings(this.settingsCopy);
    }
  }

  public enableBetaVersions(value: boolean): void {
    this.settingsCopy.enableBeta = value;
    this.saveSettings(this.settingsCopy);
  }

  public enableAnalytics(value: boolean): void {
    this.settingsCopy.enableAnalytics = value;
    this.saveSettings(this.settingsCopy);
  }

  public enableList(value: boolean): void {
    this.settingsCopy.listMode = value;
    this.saveSettings(this.settingsCopy);
  }

  public enableVerbose(value: boolean): void {
    this.settingsCopy.verbose = value;
    this.saveSettings(this.settingsCopy);
  }

  public enableAutomate(value: boolean): void {
    this.settingsCopy.automateMojang = value;
    this.saveSettings(this.settingsCopy);
  }

  public doSave() {
    if (this.settingsCopy.packCardSize > 2 && this.settingsCopy.listMode) {
      this.settingsCopy.listMode = false;
    }
    this.saveSettings(this.settingsCopy);
  }

  public exitOverwolf(value: boolean): void {
    this.settingsCopy.exitOverwolf = value;
    this.saveSettings(this.settingsCopy);
    platform.get.actions.changeExitOverwolfSetting(value);
  }

  get resolutionList() {
    const resList = [];
    for (const [key, res] of Object.entries(this.settingsState.hardware.supportedResolutions)) {
      resList.push({ id: key, name: res.width + 'x' + res.height, value: key });
    }
    return resList;
  }
}
</script>

<style scoped lang="scss">
.settings {
  height: 100%;
  display: flex;

  > main {
    flex: 1;
    padding: 2rem 3rem;
    height: 100%;
    overflow-y: auto;
  }
}
</style>
