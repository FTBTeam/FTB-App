<template>
  <div id="app" class="theme-dark" v-if="platfrom.get.config">
    <title-bar />
    <div class="app-container" v-if="websockets.socket.isConnected && !loading">
      <main class="main">
        <sidebar v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
          <div class="bottom-area">
            <div v-if="modpacks.installing != null">
              <div class="progress-bar relative">
                <div class="pl-4 w-full" v-bind:style="{ position: 'absolute' }" v-if="modpacks.installing.error">
                  Error installing {{ modpacks.installing.pack.name }} - {{ modpacks.installing.errorMessage }} -
                  <button
                    class="bg-orange-500 hover:bg-orange-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"
                    @click="retry(modpacks.installing)"
                  >
                    <span class="cursor-pointer">Retry?</span>
                  </button>
                </div>
                <p
                  class="pl-4 w-full"
                  v-bind:style="{ position: 'absolute' }"
                  v-else-if="modpacks.installing.stage === 'INIT'"
                >
                  Initialising install...
                </p>
                <p
                  class="pl-4 w-full"
                  v-bind:style="{ position: 'absolute' }"
                  v-else-if="modpacks.installing.stage === 'PREPARE'"
                >
                  Downloading modpack metadata...
                </p>
                <p
                  class="pl-4 w-full"
                  v-bind:style="{ position: 'absolute' }"
                  v-else-if="modpacks.installing.stage === 'MODLOADER'"
                >
                  Installing Mod Loader...
                </p>
                <p
                  class="pl-4 w-full"
                  v-bind:style="{ position: 'absolute' }"
                  v-else-if="modpacks.installing.stage === 'DOWNLOADS'"
                >
                  Installing {{ modpacks.installing.pack.name }} - {{ modpacks.installing.progress.toFixed(2) }}% ({{
                    (modpacks.installing.downloadSpeed / 1000000).toFixed(2)
                  }}
                  mbps)
                </p>
                <p
                  class="pl-4 w-full"
                  v-bind:style="{ position: 'absolute' }"
                  v-else-if="modpacks.installing.stage == 'FINISHED'"
                >
                  Install Finished
                </p>
                <div class="w-full h-full bg-grey-light justify-center">
                  <div
                    v-if="!modpacks.installing.error"
                    class="h-full bg-primary text-xs leading-none py-1 text-white"
                    v-bind:style="{ width: `${modpacks.installing.progress}%`, transition: 'width 0.5s ease' }"
                  ></div>
                  <div
                    v-else
                    class="h-full bg-error-button text-xs leading-none py-1 text-white"
                    v-bind:style="{ width: `100%`, transition: 'width 0.5s ease' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <ad-aside />
      </main>
    </div>
    <div class="app-container centered" v-else>
      <div class="pushed-content">
        <report-form
          v-if="(!websockets.firstStart && !loading) || websockets.reconnects > 20"
          :loadingFailed="loading"
          :websocketsFailed="!websockets || websockets.reconnects > 20"
          :websockets="websockets"
          :max-tries="20"
        />
        <div
          class="container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col"
          style="flex-direction: column; justify-content: center; align-items: center"
          v-else
        >
          <div class="background-animation"></div>
          <img src="../assets/logo_ftb.png" width="300" class="loader-logo-animation" />
          <div class="progress">
            <div class="bar"></div>
          </div>
        </div>
      </div>
    </div>

    <global-components />
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { SocketState } from '@/modules/websocket/types';
import FTBModal from '@/components/atoms/FTBModal.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { logVerbose } from '@/utils';
import { InstallProgress, ModpackState } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';
import platfrom from '@/utils/interface/electron-overwolf';
import ReportForm from '@/components/templates/ReportForm.vue';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/templates/GlobalComponents.vue';

@Component({
  components: {
    GlobalComponents,
    Sidebar,
    TitleBar,
    FTBModal,
    ReportForm,
    'message-modal': MessageModal,
    AdAside,
  },
})
export default class MainApp extends Vue {
  @State('websocket') public websockets!: SocketState;
  @State('modpacks') public modpacks!: ModpackState;
  @State('settings') public settings!: SettingsState;
  @Action('sendMessage') public sendMessage: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;
  @Action('disconnect') public disconnect: any;
  private loading: boolean = false;
  private hasLoaded: boolean = false;

  @Action('registerExitCallback') private registerExitCallback: any;
  @Action('registerPingCallback') private registerPingCallback: any;

  @Action('loadProfiles', { namespace: 'core' }) private loadProfiles!: any;

  private platfrom = platfrom;

  private windowId: string | null = null;

  public mounted() {
    this.registerPingCallback((data: any) => {
      if (data.type === 'ping') {
        this.sendMessage({ payload: { type: 'pong' } });
      }
    });

    this.platfrom.get.frame.setupTitleBar((windowId) => (this.windowId = windowId));

    // Only used on overwolf.
    this.registerExitCallback((data: any) => {
      if (data.type === 'yeetLauncher') {
        this.platfrom.get.actions.yeetLauncher(this.windowId, () => {
          this.saveSettings(this.settings?.settings);
          this.disconnect();
        });
      }
    });
  }

  @Watch('websockets', { deep: true })
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && !this.loading && !this.hasLoaded) {
      this.loading = true;
      await this.fetchStartData();
      this.hasLoaded = true;
      this.loading = false;
      this.platfrom.get.actions.onAppReady();
    } else if (!newVal.socket.isConnected) {
      this.hasLoaded = false;
    }
  }

  public fetchStartData() {
    return new Promise(async (resolve, reject) => {
      await this.loadSettings();
      this.loadProfiles();
      this.sendMessage({
        payload: { type: 'installedInstances' },
        callback: (data: any) => {
          this.storePacks(data);
          resolve(null);
        },
      });
    });
  }

  public retry(modpack: InstallProgress) {
    if (this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID).length > 0) {
      logVerbose(this.settings, 'The instance already exists, assume an update.');
    } else {
      logVerbose(this.settings, 'Instance does not exist, we can assume delete is fine');
      const foundPack = this.modpacks.installedPacks.filter((pack) => pack.uuid === modpack.instanceID)[0];
      if (!foundPack) {
        // TODO: Fix me, handle this issue
        return;
      }
      this.sendMessage({
        payload: { type: 'uninstallInstance', uuid: foundPack.uuid },
        callback: (data: any) => {
          this.sendMessage({
            payload: { type: 'installedInstances', refresh: true },
            callback: (data: any) => {
              this.storePacks(data);
              this.updateInstall({ modpackID: foundPack.id, progress: 0 });
              this.sendMessage({
                payload: { type: 'installInstance', id: foundPack.id, version: foundPack.versionId },
                callback: (data: any) => {
                  if (data.status === 'success') {
                    this.sendMessage({
                      payload: { type: 'installedInstances', refresh: true },
                      callback: (data: any) => {
                        this.storePacks(data);
                        this.finishInstall({ modpackID: foundPack.id, messageID: data.requestId });
                      },
                    });
                  } else if (data.status === 'error') {
                    this.updateInstall({
                      modpackID: foundPack.id,
                      messageID: data.requestId,
                      error: true,
                      errorMessage: data.message,
                      instanceID: data.uuid,
                    });
                  } else if (data.status === 'init') {
                    this.updateInstall({
                      modpackID: foundPack.id,
                      messageID: data.requestId,
                      stage: 'INIT',
                      message: data.message,
                    });
                  } else if (data.overallPercentage <= 100) {
                    this.updateInstall({
                      modpackID: foundPack.id,
                      messageID: data.requestId,
                      progress: data.overallPercentage,
                      downloadSpeed: data.speed,
                      downloadedBytes: data.currentBytes,
                      totalBytes: data.overallBytes,
                      stage: data.currentStage,
                    });
                  }
                  logVerbose(this.settings, 'Update Data', JSON.stringify(data));
                },
              });
            },
          });
        },
      });
    }
  }

  get showSidebar() {
    return !this.$route.path.startsWith('/settings');
  }
}
</script>

<style lang="scss" scoped>
.progress {
  margin-top: 4rem;
  width: 350px;
  height: 10px;
  background: rgba(gray, 0.2);
  border-radius: 10px;
  overflow: hidden;
  position: relative;

  .bar {
    width: 100%;
    height: 100%;
    background: var(--color-primary-button);
    position: absolute;
    left: -100%;

    animation: leftToRight 1.5s ease-in-out infinite;

    @keyframes leftToRight {
      0% {
        left: -100%;
      }
      50% {
        left: 0;
      }
      100% {
        left: 100%;
      }
    }
  }
}

.bottom-area {
  position: absolute;
  width: 100%;
  left: 0;
  bottom: 0;
}
</style>

<style lang="scss">
.app-container {
  height: calc(100% - 2rem);
  position: relative;

  &.centered {
    display: flex;
    align-items: center;
    justify-content: center;

    .pushed-content {
      margin-top: -5rem;
    }
  }
}

main.main {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
}

.app-content {
  flex: 1;
  min-height: 100%;
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
  transition: all 0.5s ease;
}
.slide-down-up-leave-active {
  transition: all 0.5s ease;
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
  background-image: url('../assets/ftb-tiny-desat.png');
  filter: brightness(0.5);
  animation-name: background-animation;
  animation-duration: 10s;
  animation-iteration-count: 1;
  animation-fill-mode: forwards;
  width: 100%;
  position: absolute;
  top: 0;
  left: 0;
  mask-image: -webkit-gradient(linear, left top, left bottom, from(rgba(0, 0, 0, 0.4)), to(rgba(0, 0, 0, 0)));
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
