<template>
  <div class="card-overview-container w-full">
    <div v-if="settingsState !== undefined" class="mb-6 card-list w-full h-size-1 shadow-lg">
      <div
        v-if="(!fake && (currentModpack !== undefined || instance !== undefined)) || isDemo"
        style="height: 100%"
        class="flex flex-row card-contents"
      >
        <div class="pack-bg" :style="`background: url(${getBackground(art)});`"></div>
        <article :class="`relative overflow-hidden mr-4`" style="height: 100%" @click="cardClicked">
          <img
            class="pack-image rounded"
            :src="getLogo(art)"
            alt="placeholder"
            :class="kind === 'cloudInstance' ? 'cloud-pack-image' : ''"
          />
          <div class="content cursor-pointer">
            <!--        <div class="name-box">{{name}} (v{{version}})</div>-->
            <div v-if="instance && !isLatestVersion && kind === 'instance'" class="update-box">New Version</div>
            <div v-if="installing" class="update-box">Installing...</div>
          </div>
        </article>
        <div class="flex-1 p-2 flex flex-col" @click="cardClicked">
          <div class="flex flex-row">
            <div class="name-box font-bold">
              <span v-if="featured" class="rounded mr-2 text-sm bg-yellow-300 px-2 uppercase text-black">Featured</span
              >{{ name }}
              <div class="inline-block ml-2 text-sm text-muted font-medium" v-if="authors.length > 0">
                By
                <template v-if="typeof authors === 'object'">
                  <span v-for="author in authors" :key="author.id"
                    >{{ author.name }} {{ authors.length > 1 ? ', ' : '' }}</span
                  >
                </template>
                <template v-else>
                  <span>{{ authors.name }}</span>
                </template>
              </div>
            </div>
          </div>

          <p class="mb-auto max-2-lines mr-4">{{ description }}</p>
          <div v-if="tags" class="flex flex-row items-center">
            <div class="flex flex-row" @click.stop>
              <span
                v-for="(tag, i) in limitedTags"
                :key="`tag-${i}`"
                @click="clickTag(tag.name)"
                class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                style="font-variant: small-caps"
                >{{ tag.name }}</span
              >
              <span
                v-if="tags.length > 5"
                :key="`tag-more`"
                class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                style="font-variant: small-caps"
                >+{{ tags.length - 5 }}</span
              >
            </div>
          </div>
        </div>
        <div
          style="width: 50px"
          class="flex flex-col list-action-button-holder"
          v-if="installed && kind === 'instance'"
        >
          <FTBButton
            @click="checkMemory()"
            :isRounded="false"
            color="primary"
            class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-tr rounded-br"
            :disabled="loading"
          >
            <span v-if="!loading"
              ><font-awesome-icon icon="play" size="sm" class="cursor-pointer" />
              <p>Play</p></span
            >
            <font-awesome-icon
              v-else-if="loading"
              :icon="'spinner'"
              size="sm"
              :class="`cursor-pointer button hover-scale lg:text-1xl sm:text-base`"
              spin
            />
          </FTBButton>
          <!-- <FTBButton @click="goToInstance" :isRounded="false" color="info" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-br"><font-awesome-icon icon="ellipsis-h" size="sm" class="cursor-pointer"/><p>More</p></FTBButton> -->
        </div>
        <div style="width: 50px" class="flex flex-col list-action-button-holder" v-if="!installed">
          <FTBButton
            @click="openInstall"
            :isRounded="false"
            color="primary"
            class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded"
            ><font-awesome-icon icon="download" size="sm" class="cursor-pointer" />
            <p>Get</p></FTBButton
          >
          <!-- <FTBButton @click="openInfo" :isRounded="false" color="info" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-br"><font-awesome-icon icon="ellipsis-h" size="sm" class="cursor-pointer"/><p>More</p></FTBButton> -->
        </div>
        <div style="width: 50px" class="flex flex-col list-action-button-holder" v-if="kind === 'cloudInstance'">
          <FTBButton
            @click="sync"
            :isRounded="false"
            color="primary"
            class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-tr rounded-br"
            ><font-awesome-icon icon="cloud-download-alt" size="sm" class="cursor-pointer" />
            <p>Sync</p></FTBButton
          >
        </div>
      </div>
    </div>
    <FTBModal :visible="showInstall" size="medium" @dismiss-modal="hideInstall" :dismissable="true">
      <InstallModal :pack-name="name" :doInstall="install" :pack-description="description" :versions="versions" />
    </FTBModal>
    <FTBModal :visible="showMsgBox" @dismiss-modal="hideMsgBox" :dismissable="true">
      <message-modal
        :title="msgBox.title"
        :content="msgBox.content"
        :ok-action="msgBox.okAction"
        :cancel-action="msgBox.cancelAction"
        :type="msgBox.type"
      />
    </FTBModal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import SettingsModal from '@/components/organisms/modals/SettingsModal.vue';
import InformationModal from '@/components/organisms/modals/InformationModal.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { Action, State } from 'vuex-class';
import { Instance, ModpackState } from '../../../modules/modpacks/types';
// @ts-ignore
import * as placeholderImage from '@/assets/placeholder_art.png';
import semver from 'semver';
import { SettingsState } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';
import { logVerbose } from '@/utils';
import { RouterNames } from '@/router';
import { InstallerState } from '@/modules/app/appStore.types';

export interface MsgBox {
  title: string;
  content: string;
  type: string;
  okAction: () => void;
  cancelAction: () => void;
}

@Component({
  components: {
    FTBButton,
    FTBModal,
    SettingsModal,
    InformationModal,
    InstallModal,
    'message-modal': MessageModal,
  },
  props: [
    'packID',
    'art',
    'name',
    'authors',
    'installed',
    'version',
    'versionID',
    'minecraft',
    'description',
    'instanceID',
    'versions',
    'instance',
    'fake',
    'isDemo',
    'size',
    'tags',
    'preLaunch',
    'type',
    'postLaunch',
    'kind',
    'featured',
  ],
})
export default class PackCardList extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @State('auth') public auth!: AuthState;
  @Action('sendMessage') public sendMessage: any;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall: any;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall: any;
  @Action('errorInstall', { namespace: 'modpacks' }) public errorInstall: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks: any;
  @State('settings') public settingsState!: SettingsState;
  @Action('doSearch', { namespace: 'modpacks' }) public doSearch: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  public name!: string;
  @Prop() public instance!: Instance;
  @Prop() public packID!: number;
  @Prop() public tags!: [];
  @Prop() public type!: string;
  @Prop() public preLaunch!: (id: Instance) => Promise<void>;
  @Prop() public postLaunch!: (id: Instance) => Promise<void>;
  @Prop() public kind!: string;
  private showInstall: boolean = false;
  private showMsgBox: boolean = false;
  private defaultImage: any = placeholderImage;
  private msgBox: MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };
  private loading: boolean = false;

  @Watch('modpacks', { deep: true })
  public onModpacksChange(newState: ModpackState, oldState: ModpackState) {
    logVerbose(this.settingsState, 'State updated');
    this.$forceUpdate();
  }

  public clickTag(tagName: string) {
    this.$router.push({ name: 'browseModpacks', params: { search: tagName } });
  }

  public async mounted() {
    if (this.instance !== undefined) {
      this.instance.packType == 0 ? this.fetchModpack(this.instance.id) : this.fetchCursepack(this.instance.id);
    }
  }

  public async sync() {
    if (this.modpacks.installing !== null) {
      return;
    }
    this.updateInstall({ modpackID: this.$props.packID, progress: 0 });
    this.sendMessage({
      payload: { type: 'syncInstance' },
      callback: (data: any) => {
        if (this.showInstall) {
          this.showInstall = false;
        }
        if (data.status === 'success') {
          this.sendMessage({
            payload: { type: 'installedInstances', refresh: true },
            callback: (data: any) => {
              this.storePacks(data);
              this.finishInstall({ modpackID: this.$props.packID, messageID: data.requestId });
            },
          });
        } else if (data.status === 'error') {
          this.updateInstall({
            modpackID: this.$props.packID,
            messageID: data.requestId,
            error: true,
            errorMessage: data.message,
            instanceID: data.uuid,
          });
        } else if (data.currentStage === 'POSTINSTALL') {
          this.updateInstall({
            modpackID: this.$props.packID,
            messageID: data.requestId,
            stage: data.currentStage,
          });
        } else if (data.status === 'init') {
          this.updateInstall({
            modpackID: this.$props.packID,
            messageID: data.requestId,
            stage: 'INIT',
            message: data.message,
          });
        } else if (data.overallPercentage <= 100) {
          this.updateInstall({
            modpackID: this.$props.packID,
            messageID: data.requestId,
            progress: data.overallPercentage,
            downloadSpeed: data.speed,
            downloadedBytes: data.currentBytes,
            totalBytes: data.overallBytes,
            stage: data.currentStage,
          });
        }
      },
    });
  }

  public checkMemory() {
    if (this.instance.memory < this.instance.minMemory) {
      this.msgBox.type = 'okCancel';
      this.msgBox.title = 'Low Memory';
      this.msgBox.okAction = this.launch;
      this.msgBox.cancelAction = this.hideMsgBox;
      this.msgBox.content =
        `You are trying to launch the modpack with memory settings that are below the` +
        `minimum required. This may cause the modpack to not start or crash frequently.<br>We recommend that you` +
        `increase the assigned memory to at least **${this.instance?.minMemory}MB**\n\nYou can change the memory by going to the settings tab of the modpack and adjusting the memory slider`;
      this.showMsgBox = true;
    } else {
      this.launch();
    }
  }

  public async launch(): Promise<void> {
    await this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance?.uuid },
    });
  }

  get limitedTags() {
    if (this.tags) {
      return this.tags.slice(0, 5);
    } else {
      return [];
    }
  }

  public hideMsgBox(): void {
    this.showMsgBox = false;
  }

  get installing() {
    return (
      this.modpacks !== undefined &&
      this.modpacks.installing !== null &&
      this.modpacks.installing.modpackID === this.$props.packID
    );
  }

  public async install(version: number, versionName: string): Promise<void> {
    if (this.modpacks.installing !== null) {
      return;
    }

    this.installModpack({
      pack: {
        id: this.$props.packID,
        version: version,
        packType: this.$props.type,
      },
      meta: {
        name: this.$props.name,
        version: versionName,
        art: this.getLogo(this.$props.art),
      },
    });

    this.showInstall = false;
  }

  public goToInstance(): void {
    this.$router.push({ name: 'instancepage', query: { uuid: this.$props.instanceID } });
  }

  public openInfo(): void {
    this.$router.push({ name: 'modpackpage', query: { modpackid: this.$props.packID, type: this.$props.type ?? 0 } });
  }

  public openInstall(): void {
    this.showInstall = true;
  }

  public hideInstall(): void {
    this.showInstall = false;
  }

  get latestVersion() {
    try {
      return this.modpacks?.packsCache[this.instance.id].versions.sort((a, b) => {
        return semver.rcompare(a.name, b.name);
      });
    } catch (e) {
      console.log('Error getting latest version, semver failed. Falling back to reverse', e);
      return this.modpacks?.packsCache[this.instance.id].versions.reverse();
    }
  }

  get currentModpack() {
    if (this.instance) {
      if (this.modpacks?.packsCache[this.instance?.id] === undefined) {
        return null;
      }
      return this.modpacks?.packsCache[this.instance?.id];
    } else {
      return this.modpacks?.packsCache[this.packID];
    }
  }

  get isLatestVersion() {
    if (this.currentModpack === undefined || this.currentModpack === null) {
      return true;
    }
    return this.instance.versionId === this.currentModpack?.versions[0].id;
  }

  public getLogo(packArt: any) {
    if (typeof packArt === 'string') return packArt;
    let artP = packArt.filter((art: any) => art.type === 'square' || art.type === 'logo')[0];
    if (artP === undefined) {
      return placeholderImage;
    }
    return artP.url;
  }

  public getBackground(packArt: any) {
    if (typeof packArt === 'string') return '';
    let artP = packArt.filter((art: any) => art.type === 'splash')[0];
    if (artP === undefined) {
      return '';
    }
    return artP.url;
  }

  public cardClicked() {
    if (this.kind === 'instance') {
      this.goToInstance();
    } else {
      this.openInfo();
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  position: relative;
}

.card-list {
  background: var(--color-sidebar-item);
  border-radius: 5px;
  position: relative;
  z-index: 1;
  overflow: hidden;
  transition: 0.25s ease-in-out transform;

  &:hover {
    transform: translateY(-5px);
    .pack-bg {
      transform: scale(1.2);
    }
  }

  .card-contents {
    position: relative;
    padding: 1rem;

    .pack-bg {
      position: absolute;
      z-index: -1;
      left: 0;
      top: 0;
      width: 100%;
      height: 100%;
      opacity: 0.2;
      transition: 0.25s ease-in-out transform;
    }
  }
}

.pack-image {
  transition: filter 0.5s;
  height: 100%;
  object-fit: cover;
}
.cloud-pack-image {
  filter: grayscale(0.7);
}
.card-list .list-action-button {
  filter: brightness(0.7);
}
.card-list:hover .list-action-button {
  filter: brightness(1);
}
.card-list:hover .flex {
  cursor: pointer;
  .background .background-art {
    filter: saturate(1);
  }
  .bg-background-lighten {
    background-color: rgba(68, 68, 68, 0.6);
  }
}
.pack-image.blur {
  filter: blur(5px) brightness(50%);
}
.list-action-button-holder:hover .list-action-button:not(:hover) {
  height: 0;
  padding-top: 0;
  padding-bottom: 0;
  & > * {
    height: 0;
  }
}
.list-action-button {
  transition: height 0.2s, filter 0.4s;
  overflow: hidden;
  & > * {
    overflow: hidden;
  }
  & p:not(.cursor-pointer) {
    display: none;
  }
}
.list-action-button-holder .list-action-button:hover,
.list-action-button-holder .list-action-button:hover ~ .list-action-button:hover {
  height: 100%;
  & p:not(.cursor-pointer) {
    display: block;
  }
}

.buttons {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.row {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.content {
  position: absolute;
  height: 100%;
  bottom: 0;
  width: 100%;
  color: #fff;
  opacity: 1;
  transition: opacity 0.3s;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
}

.content .name-box {
  background: rgba(0, 0, 0, 0.6);
  width: 100%;
  text-align: left;
  font-size: 15px;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.content .update-box {
  background: rgba(255, 193, 7, 0.9);
  width: 100%;
  text-align: left;
  color: #000;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
  top: 0;
  position: absolute;
}

.hoverContent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  color: #fff;
  opacity: 0;
  transition: opacity 0.5s;
  z-index: 4;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  // margin: auto 0;
}

.divider {
  height: 20px;
  border-left: 1px solid #fff;
  border-right: 1px solid #fff;
  margin: 0 20px;
}

.button {
  transition: transform 0.2s ease-in;
}

.hover-scale:hover {
  transform: scale(1.3);
}

.button-shadow {
  // text-shadow: 3px 6px #272634;
  filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, 0.8));
}

.max-2-lines {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
.background {
  width: 100%;
  position: relative;
  z-index: 0;
  .background-art {
    position: absolute;
    background-repeat: no-repeat !important;
    background-size: cover !important;
    background-position: 50% !important;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: -1;
    top: 0;
    left: 0;
    opacity: 0.4;
    filter: saturate(0);
  }
  &:before {
    content: '';
    position: absolute;
    z-index: 2;
    width: 100%;
    height: 100%;
    pointer-events: none;
    top: 0px;
    left: 0px;
  }
  &:after {
    content: '';
    position: absolute;
    z-index: 3;
    width: 100%;
    height: 100%;
    pointer-events: none;
    bottom: 0px;
    left: 0px;
  }
}
</style>
