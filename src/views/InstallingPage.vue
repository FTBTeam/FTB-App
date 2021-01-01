<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div
      class="flex flex-col h-full"
      v-if="!loading && currentModpack !== null"
      key="main-window"
      v-bind:style="{
        'background-image': `url(${
          currentModpack.art.filter((art) => art.type === 'splash').length > 0
            ? currentModpack.art.filter((art) => art.type === 'splash')[0].url
            : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'
        })`,
      }"
    >
      <div>
        <div class="header-image" style="height: 120px">
          <span class="instance-name text-4xl">{{ currentModpack.name }}</span>
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
          <div
            class="update-bar"
            v-if="currentModpack && currentModpack.notification"
          >
            {{ currentModpack.notification }}
          </div>
        </div>
      </div>
      <div
        style="height: auto; flex: 1; overflow-y: auto"
        class="flex flex-col frosted-glass"
      >
        <div class="tab-content flex-1 py-4 mx-1" style="overflow-y: auto">
          <div class="tab-pane flex flex-row h-full">
            <div
              class="mod-list bg-navbar w-1/3 overflow-y-auto relative"
              ref="modList"
              id="modList"
            >
              <ul class="p-4">
                <li
                  v-for="stepID in Object.keys(steps).slice(0, 4)"
                  :class="
                    steps[stepID].done
                      ? 'downloaded text-gray-400'
                      : 'text-white'
                  "
                  :key="stepID"
                >
                  <p class="text-sm">
                    <font-awesome-icon
                      class="text-green-600"
                      v-if="steps[stepID].done"
                      icon="check"
                    ></font-awesome-icon>
                    {{ steps[stepID].name }}
                  </p>
                </li>
                <li
                  v-for="file in files"
                  :key="file.id"
                  :class="
                    getFileStatus(file.id) === 'downloaded' ||
                    steps['DOWNLOADS'].done
                      ? 'downloaded text-gray-400'
                      : 'text-white'
                  "
                  class="flex flex-row"
                >
                  <p class="text-sm">
                    <font-awesome-icon
                      class="text-green-600"
                      :spin="getFileStatus(file.id) === 'preparing'"
                      v-if="
                        getFileStatus(file.id) !== 'waiting' ||
                        steps['DOWNLOADS'].done
                      "
                      :icon="
                        getFileStatus(file.id) === 'preparing'
                          ? 'sync-alt'
                          : 'check'
                      "
                    ></font-awesome-icon>
                    {{ file.name }}
                  </p>
                </li>
                <li
                  v-for="stepID in Object.keys(steps).slice(4, 7)"
                  :class="
                    steps[stepID].done
                      ? 'downloaded text-gray-400'
                      : 'text-white'
                  "
                  :key="stepID"
                >
                  <p class="text-sm">
                    <font-awesome-icon
                      class="text-green-600"
                      v-if="steps[stepID].done"
                      icon="check"
                    ></font-awesome-icon>
                    {{ steps[stepID].name }}
                  </p>
                </li>
                <div
                  v-if="steps['FINISHED'].done"
                  class="flex flex-col align-center mx-auto text-center"
                  id="viewInstanceButton"
                  ref="viewInstanceButton"
                >
                  <p>Modpack installed!</p>
                  <ftb-button
                    class="py-2 px-4 mx-2 text-center float-right"
                    color="primary"
                    css-class="text-center text-l"
                    @click="goToInstance()"
                    >View Instance</ftb-button
                  >
                </div>
              </ul>
            </div>
            <div class="flex-1 h-full flex flex-col">
              <div
                class="flex flex-col w-full mt-auto mb-auto"
                v-if="advertsEnabled"
              >
                <div v-if="!showPlaceholder" id="ad" ref="adRef" style="max-width: 400px; max-height: 300px; display: flex; margin: 0 auto;">
                  <div id="777249406"></div>
                </div>
                <video
                  width="400"
                  height="300"
                  autoplay
                  muted
                  loop
                  style="margin: 0 auto"
                  v-if="showPlaceholder"
                >
                  <source
                    src="https://dist.modpacks.ch/windows_desktop_src_assets_CH_AD.mp4"
                    type="video/mp4"
                  />
                </video>
                <span
                  class="ml-auto mr-auto text-xs cursor-pointer"
                  style="padding-left: 315px"
                  @click="reportAdvert"
                  >Report advert</span
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

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
.canvas-hidden {
  visibility: hidden;
}
</style>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import {
  ModpackState,
  ModPack,
  Instance,
  Versions,
} from "@/modules/modpacks/types";
import { State, Action, Getter } from "vuex-class";
import FTBInput from "@/components/FTBInput.vue";
import FTBToggle from "@/components/FTBToggle.vue";
import FTBButton from "@/components/FTBButton.vue";
import FTBSlider from "@/components/FTBSlider.vue";
import config from "@/config";
import moment from "moment";
import MessageModal from "@/components/modals/MessageModal.vue";
import FTBModal from "@/components/FTBModal.vue";
import { logVerbose, shuffle } from "../utils";
import { SettingsState } from "../modules/settings/types";
import { ServersState } from "@/modules/servers/types";
import ServerCard from "@/components/ServerCard.vue";
import InstallModal from "@/components/modals/InstallModal.vue";
import { SocketState } from "../modules/websocket/types";
import { AuthState } from "@/modules/auth/types";

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
  name: "InstallingPage",
  components: {
    "ftb-input": FTBInput,
    "ftb-toggle": FTBToggle,
    "ftb-slider": FTBSlider,
    "ftb-button": FTBButton,
    InstallModal,
    FTBModal,
    "message-modal": MessageModal,
    ServerCard,
  },
})
export default class InstallingPage extends Vue {
  get limitedTags() {
    if (this.currentModpack && this.currentModpack.tags) {
      return this.currentModpack.tags.slice(0, 5);
    } else {
      return [];
    }
  }

  get currentModpack() {
    const id: number = parseInt(this.$route.query.modpackid as string, 10);
    if (this.modpacks.packsCache[id] === undefined) {
      return null;
    }
    return this.modpacks.packsCache[id];
  }

  get advertsEnabled(): boolean {
    return (
      this.settings.settings.showAdverts === true ||
      this.settings.settings.showAdverts === "true" ||
      this.auth?.token?.activePlan === null
    );
  }

  get currentVersionObject(): Versions | null {
    if (this.currentModpack !== null) {
      const version = this.currentModpack?.versions.find(
        (f: Versions) => f.id === this.latestRelease
      );
      if (version !== undefined) {
        return version;
      }
    }
    return null;
  }

  get shuffledServers() {
    if (
      this.currentVersionObject !== null &&
      this.currentVersionObject.mtgID !== undefined
    ) {
      return shuffle(
        this.serverListState.servers[this.currentVersionObject.mtgID]
      );
    }
    return [];
  }

  get latestRelease() {
    if (this.currentModpack !== undefined) {
      const version = this.currentModpack?.versions.find(
        (f: Versions) => f.type.toLowerCase() === "release"
      );
      if (version !== undefined) {
        return version.id;
      }
      return null;
    }
    return null;
  }

  @State("modpacks") public modpacks!: ModpackState;
  @State("settings") public settings!: SettingsState;
  @State("websocket") public socket!: SocketState;
  @State("auth") public auth!: AuthState;
  @Action("fetchModpack", { namespace: "modpacks" }) public fetchModpack!: any;
  @Action("storeInstalledPacks", { namespace: "modpacks" })
  public storePacks!: any;
  @Action("updateInstall", { namespace: "modpacks" })
  public updateInstall!: any;
  @Action("finishInstall", { namespace: "modpacks" })
  public finishInstall!: any;
  @Action("sendMessage") public sendMessage!: any;
  @Action("reportAdvert") public reportAd!: any;
  @Action("getChangelog", { namespace: "modpacks" }) public getChangelog!: any;
  @State("servers") public serverListState!: ServersState;
  @Action("fetchServers", { namespace: "servers" }) public fetchServers!: (
    projectid: string
  ) => void;
  @Getter("getFileStatus") public getFileStatus!: (name: string) => string;
  public showAdverts: boolean = false;

  private activeTab = "overview";
  private showMsgBox = false;
  private showInstallBox = false;
  private installSelectedVersion: number | null = null;
  private msgBox: MsgBox = {
    title: "",
    content: "",
    type: "",
    okAction: Function,
    cancelAction: Function,
  };
  private loading = true;
  private ad: any;
  private checkAd: any;
  private files: File[] = [];
  private downloadedFiles: { [index: string]: string } = {};
  private steps: { [index: string]: { name: string; done: boolean } } = {
    INIT: { name: "Initialise", done: false },
    VANILLA: { name: "Install Vanilla launcher", done: false },
    API: { name: "Gather data from API", done: false },
    FORGE: { name: "Get Forge information", done: false },
    DOWNLOADS: { name: "Download files", done: false },
    POSTINSTALL: { name: "Post-install tasks", done: false },
    FINISHED: { name: "Finished", done: false },
  };

  private activeChangelog: number | undefined = -1;
  private changelogs: Changelogs = [];
  private showPlaceholder: boolean = false;
  private installedUUID: string | null = null;

  public goBack(): void {
    this.$router.go(-1);
  }

  public goToInstance(): void {
    if (this.installedUUID !== null) {
      this.$router.replace({
        name: "instancepage",
        query: { uuid: this.installedUUID },
      });
    }
  }

  public clickTag(tagName: string) {
    // this.$router.push({name: 'browseModpacks', params: {search: tagName}});
  }

  public show300x250(){
    var el = document.getElementById("ad");
    // @ts-ignore
    cpmstarAPI({kind:'go',module:"banner300x250",config: { target: {el:el,kind:"replace"} } });
  }

  public addAdvert() {
    try {
      // @ts-ignore
     cpmstarAPI(function(api) {
        api.game.setTarget(document.getElementById("ad"));
     });
      // @ts-ignore
     cpmstarAPI({
        kind:"game.createInterstitial",
        fail: ()  => {
          console.log("API was blocked or failed to load");
          this.showPlaceholder = true;
        }
      });
      // @ts-ignore
      cpmstarAPI({
        kind:"game.displayInterstitial",
        onAdOpened: function(){
          console.log("Interstitial opened");
        },
        onAdClosed: () => {
          this.show300x250();
        },
        fail: () => {
          this.show300x250();
        }   
      })
    } catch (error) {
      this.showPlaceholder = true;
    }
  }

  public reportAdvert() {
    let el = document.getElementById("banner300x250");
    if(!el){
      this.showPlaceholder = true;
      return;
    }
    // @ts-ignore
    const adHTML = el.children[0].contentDocument.body.innerHTML;
    // @ts-ignore
    cpmstarAPI(function(api) {
        api.game.setTarget(null);
    });
    el.innerHTML = "";
    this.ad = null;
    // @ts-ignore
    window.ad = null;
    this.showPlaceholder = true;
    this.reportAd({ object: "", html: adHTML });
  }

  private async mounted() {
    const packID: number = parseInt(this.$route.query.modpackid as string, 10);
    await this.fetchModpack(packID);
    if (this.modpacks.packsCache[packID] !== undefined) {
      this.loading = false;
    }
    if (this.modpacks.installing === null) {
      this.updateInstall({ modpackID: packID, progress: 0 });
      let isPrivate = false;
      if (this.modpacks.privatePacks.length > 0) {
        if (
          this.modpacks.privatePacks.find((m: ModPack) => m.id === packID) !==
          undefined
        ) {
          isPrivate = true;
        }
      }
      this.sendMessage({
        payload: {
          type:
            this.$route.query.uuid === undefined
              ? "installInstance"
              : "updateInstance",
          uuid: this.$route.query.uuid,
          id: packID,
          version: this.$route.query.versionID,
          _private: isPrivate,
        },
        callback: (data: any) => {
          if (data.status === "files") {
            this.files = JSON.parse(data.message);
          } else if (data.status === "success") {
            this.installedUUID = data.uuid;
            this.finishInstall({
              modpackID: packID,
              messageID: data.requestId,
            });
            const currentStageStep = this.steps.FINISHED;
            currentStageStep.done = true;
            Vue.set(this.steps, "FINISHED", currentStageStep);
            setTimeout(() => {
              let el: Element = this.$refs.viewInstanceButton as Element;
              if (el === null || el === undefined) {
                el = document.getElementById("viewInstanceButton") as Element;
                if (el === null || el === undefined) {
                  return;
                }
              }
              el.scrollIntoView({ behavior: "smooth" });
            }, 500);
            this.sendMessage({
              payload: { type: "installedInstances", refresh: true },
              callback: (data: any) => {
                this.storePacks(data);
              },
            });
          } else if (data.status === "error") {
            this.updateInstall({
              modpackID: packID,
              messageID: data.requestId,
              error: true,
              errorMessage: data.message,
              instanceID: data.uuid,
            });
          } else if (data.currentStage === "POSTINSTALL") {
            this.updateInstall({
              modpackID: packID,
              messageID: data.requestId,
              stage: data.currentStage,
            });
          } else if (data.status === "init") {
            this.updateInstall({
              modpackID: packID,
              messageID: data.requestId,
              stage: "INIT",
              message: data.message,
            });
          } else if (data.overallPercentage <= 100) {
            this.updateInstall({
              modpackID: packID,
              messageID: data.requestId,
              progress: data.overallPercentage,
              downloadSpeed: data.speed,
              downloadedBytes: data.currentBytes,
              totalBytes: data.overallBytes,
              stage: data.currentStage,
            });
          }
          if (data.currentStage) {
            const currentStageStep = this.steps[data.currentStage];
            if (currentStageStep !== undefined) {
              const index = Object.keys(this.steps).indexOf(data.currentStage);
              for (let i = 0; i < index; i++) {
                const s = this.steps[Object.keys(this.steps)[i]];
                if (!s.done) {
                  Vue.set(s, "done", true);
                }
              }
            }
          }
        },
      });
    }
    if (this.advertsEnabled) {
      setTimeout(() => {
        this.addAdvert();
        // this.ad.addEventListener('error', () => {
        //   this.showPlaceholder = true;
        // });
        // this.ad.addEventListener('impression', () => {
        //   fetch(`${config.apiURL}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/video`);
        // });
        // this.ad.addEventListener('display_ad_loaded', () => {
        //   fetch(`${config.apiURL}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/static`);
        // });
      }, 500);
    }
    setInterval(() => {
      if (
        this.modpacks.installing !== null &&
        this.files.length > 0 &&
        !this.steps.FINISHED.done
      ) {
        const list: Element = this.$refs.modList as Element;
        if (list !== undefined) {
          const el: Element = list.lastChild as Element;
          const lastEl: Element = Array.from(el.children)
            .filter((e: Element) => e.classList.contains("downloaded"))
            .reverse()[0];
          if (lastEl !== null && lastEl !== undefined) {
            lastEl.scrollIntoView({ behavior: "smooth" });
          }
        }
      }
    }, 500);
  }
}
</script>
