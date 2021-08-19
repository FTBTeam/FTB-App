<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div
      class="flex flex-col h-full"
      v-if="!loading && currentModpack !== null"
      key="main-window"
      v-bind:style="{
        'background-image': `url(${
          currentModpack.art.filter(art => art.type === 'splash').length > 0
            ? currentModpack.art.filter(art => art.type === 'splash')[0].url
            : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'
        })`,
      }"
    >
      <div>
        <div class="header-image" style="height: 120px">
          <span class="instance-name text-4xl">{{ instance.name }}</span>
          <span class="instance-info">
            <small>
              <em>{{ currentModpack.synopsis }}</em>
            </small>
            <div v-if="currentModpack.tags" class="flex flex-row items-center">
              <div class="flex flex-row">
                <span
                  v-for="(tag, i) in limitedTags"
                  :key="`tag-${i}`"
                  @click="clickTag(tag.name)"
                  class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                  style="font-variant: small-caps"
                  >{{ tag.name }}</span
                >
                <span
                  v-if="currentModpack.tags.length > 5"
                  :key="`tag-more`"
                  class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                  style="font-variant: small-caps"
                  >+{{ currentModpack.tags.length - 5 }}</span
                >
              </div>
            </div>
          </span>
          <div class="update-bar" v-if="currentModpack && currentModpack.notification">
            {{ currentModpack.notification }}
          </div>
        </div>
      </div>
      <div style="height: auto; flex: 1; overflow-y: auto" class="flex flex-col frosted-glass">
        <div class="tab-content flex-1 py-4 mx-1" style="overflow-y: auto">
          <div class="tab-pane flex flex-col h-full w-full">
            <div class="flex-1 w-full flex flex-col" style="max-height: 270px" v-if="advertsEnabled">
              <div
                v-if="!showPlaceholder"
                id="ow-ad"
                ref="ad"
                style="max-width: 400px; max-height: 300px; display: flex; margin: 0 auto;"
              >
                <div v-if="platform.isElectron()" id="777249406"></div>
              </div>
              <video width="400" height="300" autoplay muted loop style="margin: 0 auto" v-if="showPlaceholder">
                <source src="https://dist.modpacks.ch/windows_desktop_src_assets_CH_AD.mp4" type="video/mp4" />
              </video>
              <span class="ml-auto mr-auto text-xs cursor-pointer" style="padding-left: 315px" @click="reportAdvert"
                >Report advert</span
              >
            </div>
            <div class="progress mt-10">
              <div class="w-3/4 mx-auto my-2" v-for="(bar, index) in bars" :key="index">
                <div class="pl-4 text-center w-full progress-text">
                  {{ bar.message }}
                </div>
                <div class="progress-bar h-4">
                  <div class="w-full h-full bg-grey-light justify-center">
                    <div
                      class="h-full bg-primary text-xs leading-none py-1 text-white"
                      v-bind:style="{
                        width: `${(bar.step / bar.steps) * 100}%`,
                        transition: 'width 0.5s ease',
                      }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex flex-row mx-auto w-1/2 justify-between">
              <p @click="restoreLoading" class="text-xs opacity-50 cursor-pointer hover:opacity-100">
                Show client
              </p>
              <p @click="cancelLoading" class="text-xs opacity-50 cursor-pointer hover:opacity-100">
                Cancel loading
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import FTBModal from '@/components/FTBModal.vue';
import { SettingsState } from '../modules/settings/types';
import { ServersState } from '@/modules/servers/types';
import ServerCard from '@/components/ServerCard.vue';
import InstallModal from '@/components/modals/InstallModal.vue';
import { SocketState } from '../modules/websocket/types';
import { AuthState } from '@/modules/auth/types';
import platform from '@/utils/interface/electron-overwolf';

export interface MsgBox {
  title: string;
  content: string;
  type: string;
  okAction: () => void;
  cancelAction: () => void;
}

interface Changelogs {
  [id: number]: string;
}

@Component({
  name: 'LaunchingPage',
  components: {
    'ftb-input': FTBInput,
    'ftb-toggle': FTBToggle,
    'ftb-button': FTBButton,
    InstallModal,
    FTBModal,
    'message-modal': MessageModal,
    ServerCard,
  },
})
export default class LaunchingPage extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @State('settings') public settings!: SettingsState;
  @State('websocket') public socket!: SocketState;
  @State('auth') public auth!: AuthState;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall!: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall!: any;
  @Action('sendMessage') public sendMessage!: any;
  @Action('reportAdvert') public reportAd!: any;
  @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;
  @State('servers') public serverListState!: ServersState;
  @Action('fetchServers', { namespace: 'servers' }) public fetchServers!: (projectid: string) => void;
  @Getter('getFileStatus') public getFileStatus!: (name: string) => string;

  private starAPI = (window as any).cpmstarAPI;
  private activeTab = 'overview';
  private showMsgBox = false;
  private showInstallBox = false;
  private installSelectedVersion: number | null = null;
  private msgBox: MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };
  private showPlaceholder: boolean = false;
  private loading = true;
  private ad: any;
  private checkAd: any;
  private activeChangelog: number | undefined = -1;
  private changelogs: Changelogs = [];
  private installedUUID: string | null = null;
  private showAdverts: boolean = true;

  platform = platform;

  public cancelLoading() {
    this.sendMessage({
      payload: {
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'yeet',
      },
    });
    // messageClient
  }

  public restoreLoading() {
    this.sendMessage({
      payload: {
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'show',
      },
    });
  }

  public show300x250() {
    const el = document.getElementById('ad');
    this.starAPI({ kind: 'go', module: 'banner300x250', config: { target: { el, kind: 'replace' } } });
  }

  public addAdvert() {
    try {
      this.starAPI((api: { game: { setTarget: (e: unknown) => void } }) => {
        api.game.setTarget(document.getElementById('ad'));
      });
      this.starAPI({
        kind: 'game.createInterstitial',
        fail: () => {
          console.log('API was blocked or failed to load');
          this.showPlaceholder = true;
        },
      });
      this.starAPI({
        kind: 'game.displayInterstitial',
        onAdOpened() {
          console.log('Interstitial opened');
        },
        onAdClosed: () => {
          this.show300x250();
        },
        fail: () => {
          this.show300x250();
        },
      });
    } catch (error) {
      this.showPlaceholder = true;
    }
  }

  public reportAdvert() {
    const el = document.getElementById('banner300x250');
    if (!el) {
      this.showPlaceholder = true;
      return;
    }
    // TODO: Fix
    const adHTML = (el.children[0] as any).contentDocument.body.innerHTML;
    this.starAPI((api: { game: { setTarget: (e: unknown) => void } }) => {
      api.game.setTarget(null);
    });
    el.innerHTML = '';
    this.ad = null;
    (window as any).ad = null;
    this.showPlaceholder = true;
    this.reportAd({ object: '', html: adHTML });
  }

  public async mounted() {
    if (this.instance == null) {
      return null;
    }
    await this.fetchModpack(this.instance?.id);
    if (this.modpacks.packsCache[this.instance?.id] !== undefined) {
      this.loading = false;
    }
    if (this.advertsEnabled) {
      setTimeout(() => {
        this.addAdvert();
      }, 500);
    }
  }

  get instance() {
    if (this.modpacks == null) {
      return null;
    }
    return this.modpacks.installedPacks.filter(pack => pack.uuid === this.$route.query.uuid)[0];
  }

  get limitedTags() {
    if (this.currentModpack && this.currentModpack.tags) {
      return this.currentModpack.tags.slice(0, 5);
    } else {
      return [];
    }
  }

  get bars() {
    if (this.modpacks === undefined || this.modpacks === null) {
      return [];
    }
    if (this.modpacks.launchProgress === null) {
      return [];
    }
    const bars = this.modpacks.launchProgress.filter(b => b.steps !== 1);
    return bars.length > 3 ? bars.slice(0, 3) : bars;
  }

  get currentModpack() {
    if (this.instance == null) {
      return null;
    }
    const id: number = this.instance.id;
    if (this.modpacks.packsCache[id] === undefined) {
      return null;
    }
    return this.modpacks.packsCache[id];
  }

  get advertsEnabled(): boolean {
    return (
      this.settings.settings.showAdverts === true ||
      this.settings.settings.showAdverts === 'true' ||
      this.auth?.token?.activePlan === null
    );
  }

  public screenshotToBase64(url: string) {
    return new Promise((resolve, reject) => {
      let canvas = document.createElement('canvas');
      canvas.classList.add('canvas-hidden');
      document.body.appendChild(canvas);
      var context = canvas.getContext('2d');
      if (context == null) {
        reject('');
        return;
      }
      let image = new Image();
      image.onload = function() {
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        context?.drawImage(image, 0, 0);
        context?.drawImage(image, 0, 0, image.width, image.height);
        var dataURL = canvas.toDataURL();
        resolve(dataURL);
        document.body.removeChild(canvas);
      };
      image.src = url;
    });
  }
}
</script>

<style lang="scss">
.header-image {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 200px;
  transition: all 0.2s ease-in-out;
}

.tab-pane {
  top: 0;
  height: 100%;
  overflow-y: auto;
}

.changelog-seperator {
  border: 1px solid var(--color-sidebar-item);
}

.short {
  width: 75%;
}

.instance-name {
  margin-top: auto;
  height: 45px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.instance-info {
  bottom: 50px;
  text-align: left;
  font-weight: 400;
  padding: 2px 2px 2px 6px;
}

.instance-buttons {
  background: rgba(0, 0, 0, 0.7);
  width: 100%;
  height: 50px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.instance-button {
  display: flex;
  justify-content: center;
  flex-direction: column;
  text-align: center;
}

.update-bar {
  background: rgba(255, 193, 7, 0.9);
  width: 100%;
  height: 25px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
  color: black;
}

.frosted-glass {
  backdrop-filter: blur(8px);
  background: linear-gradient(
    to top,
    rgba(36, 40, 47, 0) 0%,
    rgba(43, 57, 66, 0.2) calc(100% - 2px),
    rgba(193, 202, 207, 0.1) calc(100% - 1px),
    rgba(29, 29, 29, 0.3) 100%
  );
  //   -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,1) 50%, rgba(0,0,0,0) 100%);
}
#ow-ad iframe {
  margin: 0 auto;
  vertical-align: middle;
}
#ow-ad > div {
  height: 100%;
}
#ow-ad > div > div {
  display: flex;
  height: 100%;
  align-items: center;
}
.progress-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.canvas-hidden {
  visibility: hidden;
}
</style>
