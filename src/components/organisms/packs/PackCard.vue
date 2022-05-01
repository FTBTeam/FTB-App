<template>
  <div v-if="settingsState !== undefined">
    <div class="pack-card" v-if="currentModpack !== undefined || instance !== undefined || isDemo">
      <div class="art" @click.prevent="cardClicked">
        <span
          v-if="versionType !== 'release'"
          class="beta-tag"
          :style="{ backgroundColor: getColorForReleaseType(versionType) }"
          :class="versionType"
          >{{ versionType }}</span
        >
        <div class="has-update" v-if="instance && !isLatestVersion && kind === 'instance'">Update Available</div>
        <img
          class="w-full"
          :src="getLogo(art)"
          alt="pack art"
          :class="[installing ? 'blur' : '', kind === 'cloudInstance' ? 'cloud-pack-image' : '']"
        />
      </div>
      <div class="meta">
        <div class="title">{{ name }}</div>
        <div class="version">{{ version }}</div>
        <div class="play-button" :class="{ 'opacity-50': starting }">
          <div class="clickable-play" :class="{ disabled: loading, loading }" @click="checkMemoryThenLaunch">
            <span>Play</span>
          </div>
        </div>
      </div>
    </div>
    <FTBModal :visible="showInstall" @dismiss-modal="hideInstall" :dismissable="true">
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
import placeholderImage from '@/assets/placeholder_art.png';
import semver from 'semver';
import { SettingsState } from '@/modules/settings/types';
import { logVerbose } from '@/utils';
import { AuthState } from '../../../modules/auth/types';
import { getColorForReleaseType } from '@/utils/colors';
import { RouterNames } from '@/router';

const namespace = 'websocket';

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
    'preLaunch',
    'postLaunch',
    'kind',
    'type',
  ],
})
export default class PackCard extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @State('auth') public auth!: AuthState;
  @Action('sendMessage') public sendMessage: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall: any;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack: any;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall: any;
  @Action('errorInstall', { namespace: 'modpacks' }) public errorInstall: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks: any;
  @State('settings') public settingsState!: SettingsState;
  @Action('startInstanceLoading', { namespace: 'core' }) public startInstanceLoading: any;
  @Action('stopInstanceLoading', { namespace: 'core' }) public stopInstanceLoading: any;
  @Action('showAlert') public showAlert: any;

  public name!: string;
  @Prop()
  public instance!: Instance;
  @Prop()
  public packID!: number;
  @Prop()
  public kind!: string;
  @Prop()
  public preLaunch?: (id: Instance) => Promise<void>;
  @Prop()
  public postLaunch?: (id: Instance) => Promise<void>;
  private showInstall = false;
  private showMsgBox = false;
  private defaultImage: any = placeholderImage;
  private msgBox: MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };
  private loading: boolean = false;
  private starting: boolean = false;

  getColorForReleaseType = getColorForReleaseType;

  @Watch('modpacks', { deep: true })
  public onModpacksChange(newState: ModpackState, oldState: ModpackState) {
    logVerbose(this.settingsState, 'State updated');
    this.$forceUpdate();
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
      payload: { type: 'syncInstance', uuid: this.instance.uuid },
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

  public async checkMemoryThenLaunch() {
    this.starting = true;
    if (this.instance.memory < this.instance.minMemory) {
      this.msgBox.type = 'okCancel';
      this.msgBox.title = 'Low Memory';
      this.msgBox.okAction = this.launch;
      this.msgBox.cancelAction = this.hideMsgBox;
      this.msgBox.content =
        `You are trying to launch the modpack with memory settings that are below the` +
        `minimum required.This may cause the modpack to not start or crash frequently.<br>We recommend that you` +
        `increase the assigned memory to at least **${this.instance?.minMemory}MB**\n\nYou can change the memory by going to the settings tab of the modpack and adjusting the memory slider`;
      this.showMsgBox = true;
    } else {
      await this.launch();
    }
    this.starting = false;
  }

  public async launch(): Promise<void> {
    await this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance?.uuid },
    });
  }

  public hideMsgBox(): void {
    this.showMsgBox = false;
  }

  get installing() {
    if (this.instance !== undefined && this.instance !== null) {
      return (
        this.modpacks !== undefined &&
        this.modpacks.installing !== null &&
        this.modpacks.installing.instanceID === this.instance.uuid
      );
    }
    return (
      this.modpacks !== undefined &&
      this.modpacks.installing !== null &&
      this.modpacks.installing.modpackID === this.$props.packID
    );
  }

  public install(version: number): void {
    if (this.modpacks.installing !== null) {
      return;
    }
    this.$router.replace({
      name: 'installingpage',
      query: { modpackid: this.$props.packID, versionID: version.toString(), type: this.$props.type },
    });
    this.showInstall = false;
  }

  public deleteInstace(): void {
    this.sendMessage({
      payload: { type: 'uninstallInstance', uuid: this.$props.instanceID },
      callback: (data: any) => {
        this.sendMessage({
          payload: { type: 'installedInstances', refresh: true },
          callback: (data: any) => {
            this.storePacks(data);
          },
        });
      },
    });
  }

  public goToInstance(): void {
    this.$router.push({ name: 'instancepage', query: { uuid: this.$props.instanceID } });
  }

  public openInfo(): void {
    this.$router.push({ name: 'modpackpage', query: { modpackid: this.$props.packID, type: this.$props.type } });
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
      console.log('Error getting latest version, semver failed', e);
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

  public cardClicked() {
    if (this.kind === 'instance') {
      this.goToInstance();
    } else {
      this.openInfo();
    }
  }

  get versionType() {
    return (
      this.currentModpack?.versions?.find((e) => e.id === this.instance?.versionId)?.type.toLowerCase() ?? 'release'
    );
  }
}
</script>

<style scoped lang="scss">
.pack-card {
  border-radius: 5px;
  overflow: hidden;
  border: 2px solid rgba(68, 68, 68, 0.3);
  cursor: pointer;

  &:hover {
    .play-button {
      transform: translateY(0) !important;
    }

    .title,
    .version {
      opacity: 0 !important;
    }
  }

  .art {
    position: relative;

    .beta-tag {
      position: absolute;
      top: 0.5rem;
      left: 0.5rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      background-color: rgba(234, 32, 32, 0.89);
      font-size: 0.75rem;
      font-weight: bold;

      box-shadow: 0 3px 15px rgba(black, 0.2);

      text-transform: capitalize;
    }

    .has-update {
      position: absolute;
      font-size: 0.875rem;
      color: white;
      background: rgba(41, 130, 212, 0.5);
      backdrop-filter: blur(3px);
      padding: 0.2rem 0.5rem;
      font-family: Arial, Helvetica, sans-serif;
      text-align: center;
      bottom: 0;
      width: 100%;
    }
  }

  .meta {
    position: relative;
    padding: 0.6rem 0.8rem;
    background: rgba(68, 68, 68, 0.6);

    .title {
      max-width: 100%;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
      transition: opacity 0.25s ease-in-out;
    }

    .version {
      opacity: 0.5;
      transition: opacity 0.25s ease-in-out;
      font-size: 0.875rem;
      line-height: 1em;
    }

    .play-button {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      padding: 0.75rem 0.5rem;
      transform: translateY(105%);
      transition: transform 0.25s ease-in-out;

      .clickable-play {
        background: var(--color-primary-button);
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 5px;
        transition: background-color 0.25s ease-in-out;

        &:hover {
          background: var(--color-light-primary-button);
        }
      }
    }
  }
}

.card {
  position: relative;
}

.pack-image {
  transition: filter 0.5s;
  height: 100%;
  object-fit: cover;
}

.cloud-pack-image {
  filter: grayscale(0.7);
}

.card:hover .pack-image {
  filter: blur(5px) brightness(50%);
}

.pack-image.blur {
  filter: blur(5px) brightness(50%);
}

.card {
  &:hover {
    .hoverContent {
      opacity: 1;
    }
  }
}

.hoverContent.show {
  opacity: 1;
}

// .card:hover .content {
//     opacity: 0;
// }

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
  width: 100%;
  color: #fff;
  bottom: 0;
  opacity: 1;
  transition: opacity 0.3s;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
}

.content .name-box {
  backdrop-filter: blur(8px);
  background: rgba(black, 0.2);
  text-align: left;
  width: 100%;
  font-size: 15px;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
  & p {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    max-height: 100%;
  }
}

.content .update-box {
  /*text-align: center;*/
  background: rgba(255, 193, 7, 0.9);
  width: 100%;
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
  z-index: 2;
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

// .hover-scale:hover {
//     transform: scale(1.3);
// }

.button-shadow {
  // text-shadow: 3px 6px #272634;
  filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, 0.8));
}

.action-buttons:hover .action-icon:not(:hover) {
  width: 0;
  padding-top: 0;
  padding-bottom: 0;
  & > * {
    width: 0;
  }
}
.action-icon {
  transition: width 0.2s, filter 0.4s;
  overflow: hidden;
  & > * {
    overflow: hidden;
  }
  & p.ml-2 {
    display: none;
  }
}
.action-buttons .action-icon:hover:not(.divider),
.action-buttons .action-icon:hover:not(.divider) ~ .action-icon:hover:not(.divider) {
  width: 100%;
  & p.ml-2 {
    display: block;
  }
}

.menu-icon {
  position: absolute;
  top: 3px;
  right: 5px;
  z-index: 10;
  &:hover {
    filter: drop-shadow(1px 1px 2px #000000);
  }
}

.fa-3x {
  font-size: 2.5em !important;
}
</style>
