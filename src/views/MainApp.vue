<template>
  <div id="app" class="theme-dark">
    <title-bar />
    <div class="container" v-if="websockets.socket.isConnected && !loading">
      <div id="nav" style="width: 220px;">
        <sidebar />
      </div>
      <div class="flex-1 flex flex-col overflow-x-hidden">
        <div class="content-container">
          <router-view />
          <div v-for="(modpack, index) in modpacks.installing" v-bind:key="index" >
          </div>
        </div>
        <transition name="slide-down-up">
        <div v-if="modpacks.installing != null">
          <div class="progress-bar relative" v-for="(modpack, index) in modpacks.installing" v-bind:key="index" >
            <div class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-if="modpack.error">Error installing {{modpack.pack.name}} - {{modpack.errorMessage}} - <button class="bg-orange-500 hover:bg-orange-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer" @click="retry(modpack)"><span class="cursor-pointer">Retry?</span></button></div>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'INIT'">Creating environment...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'API'">Downloading modpack metadata...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'VANILLA'">Installing Vanilla Launcher...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'FORGE'">Installing Forge...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'DOWNLOADS'">Installing {{modpack.pack.name}} - {{modpack.progress.toFixed(2)}}% ({{(modpack.downloadSpeed / 1000000).toFixed(2)}} mbps)</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'POSTINSTALL'">Finalizing Installation...</p>
            <p class="pl-4 w-full" v-bind:style="{'position': 'absolute'}" v-else-if="modpack.stage == 'FINISHED'">Install Finished</p>
            <div class=" w-full h-full bg-grey-light justify-center">
              <div v-if="!modpack.error" class="h-full bg-primary text-xs leading-none py-1 text-white" v-bind:style="{'width': `${modpack.progress}%`, 'transition': 'width 0.5s ease'}"></div>
              <div v-else class="h-full bg-error-button text-xs leading-none py-1 text-white" v-bind:style="{'width': `100%`, 'transition': 'width 0.5s ease'}"></div>
            </div>
            <!-- <p class="pl-4" v-bind:style="{'position': 'absolute', 'z-index':'0'}" >Installing {{modpack.pack.name}} - {{modpack.progress}}%</p> -->
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
              <!-- <p class="pl-4" v-bind:style="{'position': 'absolute', 'z-index':'0'}" >Installing {{modpack.pack.name}} - {{modpack.progress}}%</p> -->
            </div>
          </div>
        </transition>
      </div>
      <FTBModal v-if="$store.state.websocket.modal !== undefined && $store.state.websocket.modal !== null" :visible="$store.state.websocket.modal !== null" @dismiss-modal="hideModal">
        <message-modal :title="$store.state.websocket.modal.title" :content="$store.state.websocket.modal.message" type="custom" :buttons="$store.state.websocket.modal.buttons" :modalID="$store.state.websocket.modal.id"/>
      </FTBModal>
    </div>
    <div class=" container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col" style="flex-direction: column; justify-content: center; align-items: center;" v-else>
      <div class="background-animation"></div>
      <!-- TODO: Make this pretty -->
      <img src="../assets/logo_ftb.png" width="500px" style="margin-top: 10px;" class="loader-logo-animation"/>
      <span
      v-if="!websockets.firstStart"
      >Issue connecting to backend... Please wait or relaunch.</span>
      <span v-else>Please wait....</span>
    </div>
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/Sidebar.vue';
import TitleBar from '@/components/TitleBar.vue';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { SocketState } from '@/modules/websocket/types';
import FTBModal from '@/components/FTBModal.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import { asyncForEach, logVerbose } from '@/utils';
import {
  Instance,
  ModpackState,
  InstallProgress,
} from '@/modules/modpacks/types';
import { RootState } from '@/types';
import { SettingsState } from '../modules/settings/types';

@Component({ components: { Sidebar, TitleBar, FTBModal, 'message-modal': MessageModal} })
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

  @Watch('websockets', {deep: true})
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && !this.loading && !this.hasLoaded) {
      this.loading = true;
      await this.fetchStartData();
      this.hasLoaded = true;
      this.loading = false;
    } else if (!newVal.socket.isConnected && !this.loading) {
      this.loading = true;
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

  public retry(modpack: InstallProgress) {
    if (this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID).length > 0) {
      logVerbose(this.settings, 'The instance already exists, assume an update.');
    } else {
      logVerbose(this.settings, 'Instance does not exist, we can assume delete is fine');
      const foundPack = this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID)[0];
      this.sendMessage({payload: {type: 'uninstallInstance', uuid: foundPack.uuid}, callback: (data: any) => {
        this.sendMessage({payload: {type: 'installedInstances'}, callback: (data: any) => {
            this.storePacks(data);
            this.updateInstall({modpackID: foundPack.id, progress: 0});
            this.sendMessage({payload: {type: 'installInstance', id: foundPack.id, version: foundPack.versionId}, callback: (data: any) => {
              if (data.status === 'success') {
                this.sendMessage({payload: {type: 'installedInstances'}, callback: (data: any) => {
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
