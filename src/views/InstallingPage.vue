<template>
  <div class="installer-page px-6 py-4">You shouldn't be here...</div>
  <!--  <div class="flex flex-1 flex-col h-full overflow-hidden">-->
  <!--    <div-->
  <!--      class="flex flex-col h-full"-->
  <!--      v-if="!loading && currentModpack !== null"-->
  <!--      key="main-window"-->
  <!--      v-bind:style="{-->
  <!--        'background-image': `url(${-->
  <!--          currentModpack.art.filter(art => art.type === 'splash').length > 0-->
  <!--            ? currentModpack.art.filter(art => art.type === 'splash')[0].url-->
  <!--            : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'-->
  <!--        })`,-->
  <!--      }"-->
  <!--    >-->
  <!--      <div>-->
  <!--        <div class="header-image" style="height: 120px">-->
  <!--          <span class="instance-name text-4xl">{{ currentModpack.name }}</span>-->
  <!--          <span class="instance-info">-->
  <!--            <small>-->
  <!--              <em>{{ currentModpack.synopsis }}</em>-->
  <!--            </small>-->
  <!--            <div v-if="currentModpack.tags" class="flex flex-row items-center">-->
  <!--              <div class="flex flex-row">-->
  <!--                <span-->
  <!--                  v-for="(tag, i) in limitedTags"-->
  <!--                  :key="`tag-${i}`"-->
  <!--                  @click="clickTag(tag.name)"-->
  <!--                  class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"-->
  <!--                  style="font-variant: small-caps"-->
  <!--                  >{{ tag.name }}</span-->
  <!--                >-->
  <!--                <span-->
  <!--                  v-if="currentModpack.tags.length > 5"-->
  <!--                  :key="`tag-more`"-->
  <!--                  class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"-->
  <!--                  style="font-variant: small-caps"-->
  <!--                  >+{{ currentModpack.tags.length - 5 }}</span-->
  <!--                >-->
  <!--              </div>-->
  <!--            </div>-->
  <!--          </span>-->
  <!--          <div class="update-bar" v-if="currentModpack && currentModpack.notification">-->
  <!--            {{ currentModpack.notification }}-->
  <!--          </div>-->
  <!--        </div>-->
  <!--      </div>-->
  <!--      <div style="height: auto; flex: 1; overflow-y: auto" class="flex flex-col frosted-glass">-->
  <!--        <div class="tab-content flex-1 py-4 mx-1" style="overflow-y: auto">-->
  <!--          <div class="tab-pane flex flex-row h-full">-->
  <!--            <div class="mod-list bg-navbar w-1/3 overflow-y-auto relative" ref="modList" id="modList">-->
  <!--              <ul class="p-4">-->
  <!--                <li-->
  <!--                  v-for="stepID in Object.keys(steps).slice(0, 3)"-->
  <!--                  :class="steps[stepID].done ? 'downloaded text-gray-400' : 'text-white'"-->
  <!--                  :key="stepID"-->
  <!--                >-->
  <!--                  <p class="text-sm">-->
  <!--                    <font-awesome-icon-->
  <!--                      class="text-green-600"-->
  <!--                      v-if="steps[stepID].done"-->
  <!--                      icon="check"-->
  <!--                    ></font-awesome-icon>-->
  <!--                    {{ steps[stepID].name }}-->
  <!--                  </p>-->
  <!--                </li>-->
  <!--                <li-->
  <!--                  v-for="file in files"-->
  <!--                  :key="file.id"-->
  <!--                  :class="-->
  <!--                    getFileStatus(file.id) === 'downloaded' || steps['DOWNLOADS'].done-->
  <!--                      ? 'downloaded text-gray-400'-->
  <!--                      : 'text-white'-->
  <!--                  "-->
  <!--                  class="flex flex-row"-->
  <!--                >-->
  <!--                  <p class="text-sm">-->
  <!--                    <font-awesome-icon-->
  <!--                      class="text-green-600"-->
  <!--                      :spin="getFileStatus(file.id) === 'preparing'"-->
  <!--                      v-if="getFileStatus(file.id) !== 'waiting' || steps['DOWNLOADS'].done"-->
  <!--                      :icon="getFileStatus(file.id) === 'preparing' ? 'sync-alt' : 'check'"-->
  <!--                    ></font-awesome-icon>-->
  <!--                    {{ file.name }}-->
  <!--                  </p>-->
  <!--                </li>-->
  <!--                <li-->
  <!--                  v-for="stepID in Object.keys(steps).slice(3, 4)"-->
  <!--                  :class="steps[stepID].done ? 'downloaded text-gray-400' : 'text-white'"-->
  <!--                  :key="stepID"-->
  <!--                >-->
  <!--                  <p class="text-sm">-->
  <!--                    <font-awesome-icon-->
  <!--                      class="text-green-600"-->
  <!--                      v-if="steps[stepID].done"-->
  <!--                      icon="check"-->
  <!--                    ></font-awesome-icon>-->
  <!--                    {{ steps[stepID].name }}-->
  <!--                  </p>-->
  <!--                </li>-->
  <!--                <div-->
  <!--                  v-if="steps['FINISHED'].done"-->
  <!--                  class="flex flex-col align-center mx-auto text-center"-->
  <!--                  id="viewInstanceButton"-->
  <!--                  ref="viewInstanceButton"-->
  <!--                >-->
  <!--                  <p>Modpack installed!</p>-->
  <!--                  <ftb-button-->
  <!--                    class="py-2 px-4 mx-2 text-center float-right"-->
  <!--                    color="primary"-->
  <!--                    css-class="text-center text-l"-->
  <!--                    @click="goToInstance()"-->
  <!--                    >View Instance</ftb-button-->
  <!--                  >-->
  <!--                </div>-->
  <!--              </ul>-->
  <!--            </div>-->
  <!--            &lt;!&ndash; No ad needed atm // @michaelhillcox &ndash;&gt;-->
  <!--            &lt;!&ndash;            <div class="flex-1 h-full flex flex-col">&ndash;&gt;-->
  <!--            &lt;!&ndash;              <div class="flex flex-col w-full mt-auto mb-auto" v-if="advertsEnabled">&ndash;&gt;-->
  <!--            &lt;!&ndash;                <div&ndash;&gt;-->
  <!--            &lt;!&ndash;                  v-if="!showPlaceholder"&ndash;&gt;-->
  <!--            &lt;!&ndash;                  :id="`${platform.isElectron() ? 'ad' : 'ow-ad'}`"&ndash;&gt;-->
  <!--            &lt;!&ndash;                  ref="adRef"&ndash;&gt;-->
  <!--            &lt;!&ndash;                  style="max-width: 400px; max-height: 300px; display: flex; margin: 0 auto;"&ndash;&gt;-->
  <!--            &lt;!&ndash;                >&ndash;&gt;-->
  <!--            &lt;!&ndash;                  <div v-if="platform.isElectron()" id="777249406"></div>&ndash;&gt;-->
  <!--            &lt;!&ndash;                </div>&ndash;&gt;-->
  <!--            &lt;!&ndash;                <video width="400" height="300" autoplay muted loop style="margin: 0 auto" v-if="showPlaceholder">&ndash;&gt;-->
  <!--            &lt;!&ndash;                  <source src="https://dist.modpacks.ch/windows_desktop_src_assets_CH_AD.mp4" type="video/mp4" />&ndash;&gt;-->
  <!--            &lt;!&ndash;                </video>&ndash;&gt;-->
  <!--            &lt;!&ndash;                <span class="ml-auto mr-auto text-xs cursor-pointer" style="padding-left: 315px" @click="reportAdvert"&ndash;&gt;-->
  <!--            &lt;!&ndash;                  >Report advert</span&ndash;&gt;-->
  <!--            &lt;!&ndash;                >&ndash;&gt;-->
  <!--            &lt;!&ndash;              </div>&ndash;&gt;-->
  <!--            &lt;!&ndash;            </div>&ndash;&gt;-->
  <!--          </div>-->
  <!--        </div>-->
  <!--      </div>-->
  <!--    </div>-->
  <!--  </div>-->
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class InstallingPage extends Vue {
  // @State('modpacks') public modpacks!: ModpackState;
  // @State('settings') public settings!: SettingsState;
  // @State('websocket') public socket!: SocketState;
  // @State('auth') public auth!: AuthState;
  // @State('servers') public serverListState!: ServersState;
  //
  // @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  // @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: any;
  // @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;
  // @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall!: any;
  // @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall!: any;
  // @Action('sendMessage') public sendMessage!: any;
  // @Action('reportAdvert') public reportAd!: any;
  // @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;
  // @Action('fetchServers', { namespace: 'servers' }) public fetchServers!: (projectid: string) => void;
  // @Getter('getFileStatus') public getFileStatus!: (name: string) => string;
  // @Action('showAlert') public showAlert: any;
  //
  // platform = platform;
  //
  // public showAdverts: boolean = false;
  // private activeTab = 'overview';
  // private showMsgBox = false;
  // private showInstallBox = false;
  // private installSelectedVersion: number | null = null;
  // private msgBox: MsgBox = {
  //   title: '',
  //   content: '',
  //   type: '',
  //   okAction: Function,
  //   cancelAction: Function,
  // };
  // private loading = true;
  // private ad: any;
  // private checkAd: any;
  // private files: File[] = [];
  // private downloadedFiles: { [index: string]: string } = {};
  // private steps: { [index: string]: { name: string; done: boolean } } = {
  //   INIT: { name: 'Initialise', done: false },
  //   PREPARE: { name: 'Prepare installation', done: false },
  //   MODLOADER: { name: 'Install Mod Loader', done: false },
  //   DOWNLOADS: { name: 'Download files', done: false },
  //   FINISHED: { name: 'Finished', done: false },
  // };
  //
  // private activeChangelog: number | undefined = -1;
  // private changelogs: Changelogs = [];
  // private showPlaceholder: boolean = false;
  // private installedUUID: string | null = null;
  // private starAPI = (window as any).cpmstarAPI;
  //
  // public goBack(): void {
  //   this.$router.go(-1);
  // }
  //
  // public goToInstance(): void {
  //   if (this.installedUUID !== null) {
  //     this.$router.replace({
  //       name: 'instancepage',
  //       query: { uuid: this.installedUUID },
  //     });
  //   }
  // }
  //
  // public clickTag(tagName: string) {
  //   // this.$router.push({name: 'browseModpacks', params: {search: tagName}});
  // }
  //
  // async mounted() {
  //   const packID: number = parseInt(this.$route.query.modpackid as string, 10);
  //   const packType: number = parseInt(this.$route.query.type as string, 10);
  //   packType == 0 ? await this.fetchModpack(packID) : await this.fetchCursepack(packID);
  //   if (this.modpacks.packsCache[packID] !== undefined) {
  //     this.loading = false;
  //   }
  //   // eventBus.$on('ws.message', (data: any) => {
  //   //   console.log(data.type, data.status);
  //   //   if (data.type === 'installInstanceDataReply' && (data.status === 'abort' || data.status === 'error')) {
  //   //     this.showAlert({
  //   //       title: 'Instance failure',
  //   //       message:
  //   //         data.status === 'error'
  //   //           ? 'Unable to start pack... please see the instance logs...'
  //   //           : 'The instance has crashed or has been externally closed.',
  //   //       type: 'danger',
  //   //     });
  //   //     this.finishInstall({
  //   //       modpackID: packID,
  //   //       messageID: data.requestId,
  //   //     });
  //   //     this.$router.push({ name: RouterNames.ROOT_LIBRARY });
  //   //   }
  //   // });
  //
  //   if (this.modpacks.installing === null) {
  //     this.updateInstall({ modpackID: packID, progress: 0 });
  //     let isPrivate = false;
  //     if (this.modpacks.privatePacks.length > 0) {
  //       if (this.modpacks.privatePacks.find((m: ModPack) => m.id === packID) !== undefined) {
  //         isPrivate = true;
  //       }
  //     }
  //     this.sendMessage({
  //       payload: {
  //         type: this.$route.query.uuid === undefined ? 'installInstance' : 'updateInstance',
  //         uuid: this.$route.query.uuid,
  //         id: packID,
  //         version: this.$route.query.versionID,
  //         _private: isPrivate,
  //         packType: this.$route.query.type != null ? this.$route.query.type : 0,
  //       },
  //       callback: (data: any) => {
  //         if (data.status === 'files') {
  //           this.files = JSON.parse(data.message);
  //         } else if (data.status === 'success') {
  //           this.installedUUID = data.uuid;
  //           this.finishInstall({
  //             modpackID: packID,
  //             messageID: data.requestId,
  //           });
  //           const currentStageStep = this.steps.FINISHED;
  //           currentStageStep.done = true;
  //           Vue.set(this.steps, 'FINISHED', currentStageStep);
  //           setTimeout(() => {
  //             let el: Element = this.$refs.viewInstanceButton as Element;
  //             if (el === null || el === undefined) {
  //               el = document.getElementById('viewInstanceButton') as Element;
  //               if (el === null || el === undefined) {
  //                 return;
  //               }
  //             }
  //             el.scrollIntoView({ behavior: 'smooth' });
  //           }, 500);
  //           this.sendMessage({
  //             payload: { type: 'installedInstances', refresh: true },
  //             callback: (data: any) => {
  //               this.storePacks(data);
  //             },
  //           });
  //         } else if (data.status === 'error') {
  //           // this.updateInstall({
  //           //   modpackID: packID,
  //           //   messageID: data.requestId,
  //           //   error: true,
  //           //   errorMessage: data.message,
  //           //   instanceID: data.uuid,
  //           // });
  //           this.showAlert({
  //             title: 'Install failure',
  //             message: 'The modpack has failed to install, please check the logs.',
  //             type: 'danger',
  //           });
  //           this.finishInstall({
  //             modpackID: packID,
  //             messageID: data.requestId,
  //           });
  //           this.$router.push({ name: RouterNames.ROOT_LIBRARY });
  //         } else if (data.status === 'init') {
  //           this.updateInstall({
  //             modpackID: packID,
  //             messageID: data.requestId,
  //             stage: 'INIT',
  //             message: data.message,
  //           });
  //         } else if (data.overallPercentage <= 100) {
  //           this.updateInstall({
  //             modpackID: packID,
  //             messageID: data.requestId,
  //             progress: data.overallPercentage,
  //             downloadSpeed: data.speed,
  //             downloadedBytes: data.currentBytes,
  //             totalBytes: data.overallBytes,
  //             stage: data.currentStage,
  //           });
  //         }
  //         if (data.currentStage) {
  //           const currentStageStep = this.steps[data.currentStage];
  //           if (currentStageStep !== undefined) {
  //             const index = Object.keys(this.steps).indexOf(data.currentStage);
  //             for (let i = 0; i < index; i++) {
  //               const s = this.steps[Object.keys(this.steps)[i]];
  //               if (!s.done) {
  //                 Vue.set(s, 'done', true);
  //               }
  //             }
  //           }
  //         }
  //       },
  //     });
  //   }
  //   // Kinda dirty hack for this file
  //   // if (this.platform.isElectron()) {
  //   //   if (this.advertsEnabled) {
  //   //     setTimeout(() => {
  //   //       this.addAdvert();
  //   //       // this.ad.addEventListener('error', () => {
  //   //       //   this.showPlaceholder = true;
  //   //       // });
  //   //       // this.ad.addEventListener('impression', () => {
  //   //       //   fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/video`);
  //   //       // });
  //   //       // this.ad.addEventListener('display_ad_loaded', () => {
  //   //       //   fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/static`);
  //   //       // });
  //   //     }, 500);
  //   //   }
  //   // } else {
  //   //   setTimeout(() => {
  //   //     console.log('Loading advert');
  //   //     //@ts-ignore
  //   //     if (!OwAd) {
  //   //       this.showPlaceholder = true;
  //   //       console.log('No advert loaded');
  //   //     } else {
  //   //       //@ts-ignore
  //   //       if (window.ad) {
  //   //         console.log('Is window advert');
  //   //         //@ts-ignore
  //   //         this.ad = window.ad;
  //   //         this.ad.refreshAd();
  //   //       } else {
  //   //         console.log('Created advert');
  //   //         //@ts-ignore
  //   //         this.ad = new OwAd(document.getElementById('ow-ad'));
  //   //         //@ts-ignore
  //   //         window.ad = this.ad;
  //   //       }
  //   //       this.ad.addEventListener('error', () => {
  //   //         this.showPlaceholder = true;
  //   //       });
  //   //       this.ad.addEventListener('impression', () => {
  //   //         fetch(
  //   //           `${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/video`,
  //   //         );
  //   //       });
  //   //       this.ad.addEventListener('display_ad_loaded', () => {
  //   //         fetch(
  //   //           `${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/static`,
  //   //         );
  //   //       });
  //   //       //@ts-ignore
  //   //     }
  //   //   }, 500);
  //   // }
  //   setInterval(() => {
  //     if (this.modpacks.installing !== null && this.files.length > 0 && !this.steps.FINISHED.done) {
  //       const list: Element = this.$refs.modList as Element;
  //       if (list !== undefined) {
  //         const el: Element = list.lastChild as Element;
  //         const lastEl: Element = Array.from(el.children)
  //           .filter((e: Element) => e.classList.contains('downloaded'))
  //           .reverse()[0];
  //         if (lastEl !== null) {
  //           lastEl.scrollIntoView({ behavior: 'smooth' });
  //         }
  //       }
  //     }
  //   }, 500);
  // }
  //
  //   destroyed() {
  //   // Stop listening to events!
  //   eventBus.$off('ws.message');
  // }
  //
  // get limitedTags() {
  //   if (this.currentModpack && this.currentModpack.tags) {
  //     return this.currentModpack.tags.slice(0, 5);
  //   } else {
  //     return [];
  //   }
  // }
  //
  // get currentModpack() {
  //   const id: number = parseInt(this.$route.query.modpackid as string, 10);
  //   if (this.modpacks.packsCache[id] === undefined) {
  //     return null;
  //   }
  //   return this.modpacks.packsCache[id];
  // }
  //
  // get advertsEnabled(): boolean {
  //   return (
  //     this.settings.settings.showAdverts === true ||
  //     this.settings.settings.showAdverts === 'true' ||
  //     this.auth?.token?.activePlan === null
  //   );
  // }
  //
  // get currentVersionObject(): Versions | null {
  //   if (this.currentModpack !== null) {
  //     const version = this.currentModpack?.versions.find((f: Versions) => f.id === this.latestRelease);
  //     if (version !== undefined) {
  //       return version;
  //     }
  //   }
  //   return null;
  // }
  //
  // get shuffledServers() {
  //   if (this.currentVersionObject !== null && this.currentVersionObject.mtgID !== undefined) {
  //     return shuffle(this.serverListState.servers[this.currentVersionObject.mtgID]);
  //   }
  //   return [];
  // }
  //
  // get latestRelease() {
  //   if (this.currentModpack !== undefined) {
  //     const version = this.currentModpack?.versions.find((f: Versions) => f.type.toLowerCase() === 'release');
  //     if (version !== undefined) {
  //       return version.id;
  //     }
  //     return null;
  //   }
  //   return null;
  // }
}
</script>

<style lang="scss" scoped></style>
