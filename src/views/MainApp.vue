<template>
  <div id="app" class="theme-dark" v-if="platfrom.get.config">
    <title-bar />
    <div class="container" v-if="websockets.socket.isConnected && !loading">
      <div id="nav" style="width: 180px;">
        <sidebar />
      </div>
      <div class="flex-1 flex flex-col overflow-x-hidden">
        <div class="content-container">
          <router-view />
        </div>
        <transition name="slide-down-up">
        <div v-if="modpacks.installing != null">
          <div class="progress-bar relative"  >
            <div class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-if="modpacks.installing.error">Error installing {{modpacks.installing.pack.name}} - {{modpacks.installing.errorMessage}} - <button class="bg-orange-500 hover:bg-orange-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer" @click="retry(modpacks.installing)"><span class="cursor-pointer">Retry?</span></button></div>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'INIT'">Creating environment...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'API'">Downloading modpack metadata...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'VANILLA'">Installing Vanilla Launcher...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'FORGE'">Installing Forge...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'DOWNLOADS'">Installing {{modpacks.installing.pack.name}} - {{modpacks.installing.progress.toFixed(2)}}% ({{(modpacks.installing.downloadSpeed / 1000000).toFixed(2)}} mbps)</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'POSTINSTALL'">Finalizing Installation...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpacks.installing.stage == 'FINISHED'">Install Finished</p>
            <div class=" w-full h-full bg-grey-light justify-center">
              <div v-if="!modpacks.installing.error" class="h-full bg-primary text-xs leading-none py-1 text-white" v-bind:style="{'width': `${modpacks.installing.progress}%`, 'transition': 'width 0.5s ease'}"></div>
              <div v-else class="h-full bg-error-button text-xs leading-none py-1 text-white" v-bind:style="{'width': `100%`, 'transition': 'width 0.5s ease'}"></div>
            </div>
          </div>
        </div>
        </transition>
        <transition name="slide-down-up">
          <div v-if="$store.state && $store.state.alert != null">
            <div class="progress-bar relative" >
              <p class="pl-4 w-full absolute"><span class="font-bold">{{$store.state.alert.title}}</span> {{$store.state.alert.message}}</p>
              <div class="w-full h-full bg-grey-light justify-center ">
                <div class="h-full text-xs leading-none py-1 text-white" :class="`bg-${$store.state.alert.type}`"></div>
              </div>
              <div class="alert-close cursor-pointer" @click="hideAlert">
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12" class="pointer-events-none">
                  <line x1="0.71" y1="11.12" x2="11.11" y2="0.72" stroke-width="2" />
                  <line x1="0.77" y1="0.71" x2="11.18" y2="11.12" stroke-width="2" />
                </svg>
              </div>
            </div>
          </div>
        </transition>
      </div>
      <FTBModal v-if="$store.state.websocket.modal !== undefined && $store.state.websocket.modal !== null" :visible="$store.state.websocket.modal !== null" @dismiss-modal="hideModal" :dismissable="$store.state.websocket.modal.dismissable">
        <message-modal :title="$store.state.websocket.modal.title" :content="$store.state.websocket.modal.message" type="custom" :buttons="$store.state.websocket.modal.buttons" :modalID="$store.state.websocket.modal.id"/>
      </FTBModal>
    </div>
    <div class=" container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col" style="flex-direction: column; justify-content: center; align-items: center;" v-else-if="(!websockets.firstStart && !loading) || websockets.reconnects > 20">
      <img src="../assets/logo_ftb.png" width="200px" />
      <h1 class="text-2xl text-center">There was an error with the FTB App.</h1>
      <div v-if="!submitted" class="flex flex-col">
        <ftb-input label="Email" v-model="errorEmail"/>
        <p>Please describe what happened in the box below and submit.</p>
        <textarea class="bg-navbar border-background-darken p-2" v-model="errorDescription"></textarea>
        <ftb-button color="danger"
                    class="my-2 py-2 px-4 text-center rounded-br" @click="submitError">{{submittingError ? 'Submitting...' : 'Submit'}}</ftb-button>
      </div>
      <div v-else>
        <p>Thanks for submitting the bug report!</p>
        <ftb-button color="danger" class="my-2 py-2 px-4 text-center rounded-br" @click="quitApp">Quit</ftb-button>
      </div>
    </div>
    <div class=" container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col" style="flex-direction: column; justify-content: center; align-items: center;" v-else>
      <div class="background-animation"></div>
      <img src="../assets/logo_ftb.png" width="500px" style="margin-top: 10px;" class="loader-logo-animation"/>
    </div>
  </div>
</template>

<script lang="ts">
const emailRegex = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
import Sidebar from '@/components/Sidebar.vue';
import TitleBar from '@/components/TitleBar.vue';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { SocketState } from '@/modules/websocket/types';
import FTBModal from '@/components/FTBModal.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBInput from '@/components/FTBInput.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import { logVerbose } from '@/utils';
import config from '@/config';
import {
  ModpackState,
  InstallProgress,
} from '@/modules/modpacks/types';
import { SettingsState } from '../modules/settings/types';
import { ipcRenderer} from 'electron';
import platfrom from '@/utils/interface/electron-overwolf';

@Component({ components: { Sidebar, TitleBar, FTBModal, 'message-modal': MessageModal,
        'ftb-button': FTBButton, 'ftb-input': FTBInput} })
export default class MainApp extends Vue {
  @State('websocket') public websockets!: SocketState;
  @State('modpacks') public modpacks!: ModpackState;
  @State('settings') public settings!: SettingsState;
  @Action('sendMessage') public sendMessage: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' })
  public storePacks: any;
  @Action('updateInstall', { namespace: 'modpacks' })
  public updateInstall: any;
  @Action('finishInstall', { namespace: 'modpacks' })
  public finishInstall: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('hideAlert') public hideAlert: any;
  @Action('hideModal') public hideModal: any;
  private loading: boolean = false;
  private hasLoaded: boolean = false;

  @Action('registerPingCallback')
  private registerPingCallback: any;

  private errorEmail = '';
  private errorDescription = '';
  private submittingError = false;
  private submitted = false;

  private webVersion: string = config.webVersion;
  private appVersion: string = config.appVersion;

  private platfrom = platfrom;

  public mounted() {
    this.registerPingCallback((data: any) => {
      if (data.type === 'ping') {
        console.log('Sending pong');
        this.sendMessage({payload: {type: 'pong'}});
      }
    });
  }

  @Watch('websockets', {deep: true})
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && !this.loading && !this.hasLoaded) {
      this.loading = true;
      await this.fetchStartData();
      this.hasLoaded = true;
      this.loading = false;
      ipcRenderer.send('appReady');
    } else if (!newVal.socket.isConnected) {
      this.hasLoaded = false;
    }
  }

  public fetchStartData() {
    return new Promise(async (resolve, reject) => {
      await this.loadSettings();
      this.sendMessage({
        payload: { type: 'installedInstances' },
        callback: (data: any) => {
          this.storePacks(data);
          resolve();
      }});
    });
  }

  public async submitError() {
    if (!emailRegex.test(this.errorEmail)) {
      console.log('Email regex not passing');
      return;
    }
    if (this.errorDescription.length === 0) {
      return;
    }
    this.submittingError = true;
    const logLink = await this.uploadLogData().catch((err) => {
      if (err) {
        this.submittingError = false;
        // Show an error here...
        return;
      }
      });
    // Send request
    fetch(`https://minetogether.io/api/ftbAppError`, {method: 'PUT', body: JSON.stringify({email: this.errorEmail, logs: logLink, description: this.errorDescription})});
    this.submittingError = false;
    this.submitted = true;
  }

  public uploadLogData(): Promise<string> {
    return new Promise((resolve, reject) => {
      this.sendMessage({payload: {type: 'uploadLogs', uiVersion: this.webVersion}, callback: async (data: any) => {
              if (!data.error) {
                  const url = `https://pste.ch/${data.code}`;
                  resolve(url);
              } else {
                reject(data.error);
              }
      }});
    });
  }

  public retry(modpack: InstallProgress) {
    if (this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID).length > 0) {
      logVerbose(this.settings, 'The instance already exists, assume an update.');
    } else {
      logVerbose(this.settings, 'Instance does not exist, we can assume delete is fine');
      const foundPack = this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID)[0];
      this.sendMessage({payload: {type: 'uninstallInstance', uuid: foundPack.uuid}, callback: (data: any) => {
        this.sendMessage({payload: {type: 'installedInstances', refresh: true}, callback: (data: any) => {
            this.storePacks(data);
            this.updateInstall({modpackID: foundPack.id, progress: 0});
            this.sendMessage({payload: {type: 'installInstance', id: foundPack.id, version: foundPack.versionId}, callback: (data: any) => {
              if (data.status === 'success') {
                this.sendMessage({payload: {type: 'installedInstances', refresh: true}, callback: (data: any) => {
                  this.storePacks(data);
                  this.finishInstall({modpackID: foundPack.id, messageID: data.requestId});
                }});
              } else if (data.status === 'error') {
                this.updateInstall({modpackID: foundPack.id, messageID: data.requestId, error: true, errorMessage: data.message, instanceID: data.uuid});
              } else if (data.currentStage === 'POSTINSTALL') {
                // We don't care about this, keep progress bar showing.
              } else if (data.status === 'init') {
                this.updateInstall({modpackID: foundPack.id, messageID: data.requestId, stage: 'INIT', message: data.message});
              } else if (data.overallPercentage <= 100) {
                this.updateInstall({modpackID: foundPack.id, messageID: data.requestId, progress: data.overallPercentage, downloadSpeed: data.speed, downloadedBytes: data.currentBytes, totalBytes: data.overallBytes, stage: data.currentStage});
              }
              logVerbose(this.settings, 'Update Data', JSON.stringify(data));
            }});
          }});
      }});
    }
  }

  private quitApp() {
    ipcRenderer.send('quit_app');
  }
}
</script>

<style lang="scss">
#app {
  margin: 0;
  font-family: 'Raleway', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--color-text);
  height: 100vh;
  display: flex;
  flex-direction: column;
  .container {
    height: 100%;
    display: flex;
    flex-direction: row;
    background-color: var(--color-background);
  }
}
#nav {
  height: 100%;
  &.md\:w-10 {
    min-width: 170.297px;
  }
  &.w-10p {
    width: 15%;
  }
}

.content-container {
  flex: 1;
  overflow-y: auto;
  display: flex;
}
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  bottom: 0;
  z-index: 4;
  width: 100%;
  background-color: var(--color-navbar);
  height: 40px;
  max-height: 40px;
}
.alert-close {
  position: fixed;
  stroke: #fff;
  right: 10px;
}
.slide-down-up-enter-active {
  transition: all .5s ease;
}
.slide-down-up-leave-active {
  transition: all .5s ease;
}
.slide-down-up-enter, .slide-leave-to
/* .slide-fade-leave-active below version 2.1.8 */ {
  max-height: 0;
}
.loader-logo-animation {
  animation-name: saturation;
  animation-duration: 2.5s;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}

.background-animation {
    background-image: url("../assets/ftb-tiny-desat.png");
    filter: brightness(0.5);
    animation-name: background-animation;
    animation-duration: 10s;
    animation-iteration-count: 1;
    animation-fill-mode: forwards;
    width: 100vw;
    position: absolute;
    top: 0;
    left: 0;
    -webkit-mask-image: -webkit-gradient(linear, left top, left bottom, from(rgba(0,0,0,0.4)), to(rgba(0,0,0,0)));
}

@keyframes background-animation {
  from {
    height: 0;
  }
  to {
    height: 100vh;
  }
}

@keyframes saturation {
  from {
    filter: saturate(0);
  }
  to {
    filter: saturate(1);
  }
}
</style>
