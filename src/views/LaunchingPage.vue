<template>
  <div class="pack-loading flex flex-col h-full">
    <div
      class="loading-area flex-1"
      v-if="!loading && currentModpack !== null"
      :style="{ backgroundImage: `url(${art})` }"
    >
      <div class="loading-container flex justify-center items-center h-full">
        <div class="loading-info text-center">
          <h2 class="text-3xl font-bold mb-2">{{ instance.name }}</h2>
          <em class="mb-8 block">{{ currentModpack.synopsis }}</em>

          <div class="update-bar" v-if="currentModpack && currentModpack.notification">
            {{ currentModpack.notification }}
          </div>

          <div class="bars mb-8">
            <div class="bar mb-4" v-for="(bar, index) in bars" :key="index">
              <div class="text-center w-full text">
                {{ bar.message }}
              </div>
              <div
                class="h-full bg-primary progress-bar"
                v-bind:style="{
                  width: `${(bar.step / bar.steps) * 100}%`,
                  transition: 'width 0.5s ease',
                }"
              ></div>
            </div>
          </div>

          <div class="buttons flex items-center justify-center w-full">
            <ftb-button color="warning" @click="restoreLoading" class="py-1 px-4 mr-2 text-sm">
              Load in-game
            </ftb-button>
            <ftb-button color="warning" @click="cancelLoading" class="py-1 px-4 text-sm">
              Stop loading modpack
            </ftb-button>
          </div>
        </div>
      </div>
    </div>
    <div class="ad-area flex items-center" v-if="advertsEnabled">
      <div class="ad-box">
        <div class="ad-container">
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
          <span class="text-xs cursor-pointer report-btn opacity-50 hover:opacity-100" @click="reportAdvert"
            >Report advert</span
          >
        </div>
      </div>
      <div class="ad-message flex-1">
        <font-awesome-icon icon="heart" size="2x" class="mb-2" />
        <h3 class="text-lg font-bold mb-2">Thanks for your support!</h3>
        <p class="mb-2">
          We know the ads are a little annoying but for every ad you’re directly supporting the Feed the Beast Team and
          supporting CurseForge creators!
        </p>
        <p class="text-muted italic">
          Loading the Modpack inside this window, not only supports us, it also speeds up your packs load times! In most
          of our testing, we’ve seen visible loading time improvements!
        </p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import FTBToggle from '@/components/FTBToggle.vue';
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
    'ftb-toggle': FTBToggle,
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
    return bars;
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

  get art() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    return this.currentModpack.art.filter(art => art.type === 'splash').length > 0
      ? this.currentModpack.art.filter(art => art.type === 'splash')[0].url
      : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
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
.pack-loading {
  .loading-area {
    background-repeat: no-repeat;
    background-size: 100%;

    .loading-container {
      background: rgba(black, 0.7);
      backdrop-filter: blur(3px);

      .loading-info {
        .bars {
          width: 580px;

          .bar {
            overflow: hidden;
            position: relative;
            height: 30px;
            background: rgba(black, 0.8);
            border-radius: 5px;

            .progress-bar {
            }

            .text {
              position: absolute;
              width: 100%;
              text-align: center;
              top: 50%;
              transform: translateY(-50%);
            }
          }
        }
      }
    }
  }

  .ad-area {
    padding: 2rem;

    .ad-box {
      margin-right: 2rem;

      .ad-container {
        position: relative;
        width: 300px;
        height: 250px;
        background-color: black;
        border-radius: 5px;
      }

      .report-btn {
        position: absolute;
        bottom: -23px;
        left: 0;
        white-space: nowrap;
      }
    }

    .ad-message {
      svg {
        color: #ff4040;
      }
    }
  }
}

.update-bar {
  font-weight: 700;
  margin-bottom: 1rem;
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
</style>
