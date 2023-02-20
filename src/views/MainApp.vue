<template>
  <div id="app" class="theme-dark">
    <title-bar :is-dev="isDev" />
    <div class="app-container" v-if="websockets.socket.isConnected && !loading">
      <main class="main">
        <sidebar :is-dev="isDev" v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
        </div>
        <ad-aside :is-dev="isDev" />
      </main>
    </div>
    <div class="app-container centered" v-else>
      <div class="pushed-content">
        <report-form
          v-if="websockets.reconnects > 10 && this.loading"
          :loadingFailed="loading"
          :websocketsFailed="!websockets || websockets.reconnects > 10"
          :websockets="websockets"
          :max-tries="10"
        />
        <div
          class="container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col"
          style="flex-direction: column; justify-content: center; align-items: center"
          v-else
        >
          <img src="../assets/images/ftb-logo-full.svg" width="300" class="loader-logo-animation" />
          <div class="progress">
            <div class="bar"></div>
          </div>
          <em class="mt-6">{{ stage }}</em>
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
import { SettingsState } from '@/modules/settings/types';
import platfrom from '@/utils/interface/electron-overwolf';
import ReportForm from '@/components/templates/ReportForm.vue';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/templates/GlobalComponents.vue';
import { RouterNames } from '@/router';

@Component({
  components: {
    GlobalComponents,
    Sidebar,
    TitleBar,
    FTBModal,
    ReportForm,
    AdAside,
  },
})
export default class MainApp extends Vue {
  @State('websocket') public websockets!: SocketState;
  @State('settings') public settings!: SettingsState;
  @Action('sendMessage') public sendMessage: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;
  @Action('disconnect') public disconnect: any;
  private loading: boolean = true;
  private hasLoaded: boolean = false;

  @Action('registerExitCallback') private registerExitCallback: any;
  @Action('registerPingCallback') private registerPingCallback: any;

  @Action('loadProfiles', { namespace: 'core' }) private loadProfiles!: any;

  private platfrom = platfrom;
  private windowId: string | null = null;

  stage = 'Setting up...';
  hasInitialized = false;

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
    if (newVal.socket.isConnected && this.loading) {
      this.loading = false;
      await this.setupApp();
    }

    if (!newVal.socket.isConnected && !this.loading) {
      this.loading = true;
      this.stage = 'Attempting to reconnect to the apps agent...';
    }
  }

  async setupApp() {
    if (!this.hasInitialized) {
      await this.fetchStartData();
      this.hasInitialized = true;
    }
    this.platfrom.get.actions.onAppReady();
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

  get showSidebar() {
    return !this.$route.path.startsWith('/settings');
  }

  get isDev() {
    return this.$route.name === RouterNames.DEVELOPER;
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
  animation-duration: 1.8s;
  animation-iteration-count: infinite;
  animation-direction: alternate;
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
