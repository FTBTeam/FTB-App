<template>
  <div class="pack-page">
    <div class="pack-page-contents" v-if="instance && packInstance">
      <div
        class="background"
        :style="{
          'background-image': `url(${packSplashArt})`,
        }"
      ></div>
      <header>
        <pack-meta-heading
          @back="goBack"
          :hidePackDetails="hidePackDetails"
          :versionType="versionType"
          :instance="instance"
          :isForgePack="isForgePack"
        />

        <pack-title-header
          v-if="!hidePackDetails"
          :pack-instance="packInstance"
          :instance="instance"
          :isInstalled="true"
          :pack-name="instance.name"
        />
      </header>

      <div class="body" :class="{ 'settings-open': activeTab === tabs.SETTINGS }">
        <pack-body
          @mainAction="launchModPack()"
          @update="update"
          @tabChange="(e) => (activeTab = e)"
          @showVersion="showVersions = true"
          @searchForMods="searchingForMods = true"
          @getModList="(e) => getModList(e)"
          @backupsChanged="loadBackups"
          :pack-loading="packLoading"
          :active-tab="activeTab"
          :isInstalled="true"
          :instance="instance"
          :mods="modlist"
          :pack-instance="packInstance"
          :isLatestVersion="isLatestVersion"
          :updating-mod-list="updatingModlist"
          :backups="instanceBackups"
        />
      </div>

      <ftb-modal size="large" :visible="showVersions" @dismiss-modal="showVersions = false">
        <modpack-versions
          :versions="packInstance.versions"
          :pack-instance="packInstance"
          :instance="instance"
          :current="instance.versionId"
        />
      </ftb-modal>
    </div>

    <ftb-modal :visible="showMsgBox" @dismiss-modal="hideMsgBox">
      <message-modal
        :title="msgBox.title"
        :content="msgBox.content"
        :ok-action="msgBox.okAction"
        :cancel-action="msgBox.cancelAction"
        :type="msgBox.type"
        :loading="deleting"
      />
    </ftb-modal>

    <closable-panel
      :open="searchingForMods"
      @close="searchingForMods = false"
      :title="`Add mods to ${packInstance ? packInstance.name : ''}`"
      subtitle="You can find mods for this pack using the search area below"
    >
      <find-mods :instance="instance" @modInstalled="getModList" />
    </closable-panel>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModPack, ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import ServerCard from '@/components/organisms/ServerCard.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { SettingsState } from '@/modules/settings/types';
import { ServersState } from '@/modules/servers/types';
// @ts-ignore
import placeholderImage from '@/assets/placeholder_art.png';
import { AuthState } from '@/modules/auth/types';
import FindMods from '@/components/templates/modpack/FindMods.vue';
import { PackConst } from '@/utils/contants';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import ModpackPublicServers from '@/components/templates/modpack/ModpackPublicServers.vue';
import ModpackSettings from '@/components/templates/modpack/ModpackSettings.vue';
import PackMetaHeading from '@/components/molecules/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/molecules/modpack/PackTitleHeader.vue';
import PackBody from '@/components/molecules/modpack/PackBody.vue';
import { App } from '@/types';
import { AuthProfile } from '@/modules/core/core.types';
import { RouterNames } from '@/router';
import { InstallerState } from '@/modules/app/appStore.types';
import { getPackArt, wsTimeoutWrapperTyped } from '@/utils';
import ClosablePanel from '@/components/molecules/ClosablePanel.vue';
import { InstanceBackup, InstanceBackupsReply, InstanceBackupsRequest } from '@/typings/subprocess/instanceBackups';

export enum ModpackPageTabs {
  OVERVIEW,
  MODS,
  PUBLIC_SERVERS,
  SETTINGS,
  BACKUPS,
}

@Component({
  name: 'InstancePage',
  components: {
    ClosablePanel,
    PackTitleHeader,
    PackMetaHeading,
    ModpackSettings,
    ServerCard,
    'ftb-modal': FTBModal,
    'ftb-toggle': FTBToggle,
    'ftb-slider': FTBSlider,
    ModpackVersions,
    MessageModal,
    FindMods,
    ModpackPublicServers,
    PackBody,
  },
})
export default class InstancePage extends Vue {
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @State('settings') public settingsState!: SettingsState;
  @State('servers') public serverListState!: ServersState;
  @State('auth') public auth!: AuthState;

  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: any;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('sendMessage') public sendMessage!: any;
  @Action('showAlert') public showAlert: any;

  @Getter('getProfiles', { namespace: 'core' }) public authProfiles!: AuthProfile[];
  @Getter('getActiveProfile', { namespace: 'core' }) private getActiveProfile!: any;

  @Action('openSignIn', { namespace: 'core' }) public openSignIn: any;
  @Action('startInstanceLoading', { namespace: 'core' }) public startInstanceLoading: any;
  @Action('stopInstanceLoading', { namespace: 'core' }) public stopInstanceLoading: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  packLoading = false;

  // New stuff
  tabs = ModpackPageTabs;
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;

  private packInstance: ModPack | null = null;
  deleting: boolean = false;

  private showMsgBox: boolean = false;
  private msgBox: App.MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };

  private modlist: any = [];

  searchingForMods = false;
  updatingModlist = false;
  showVersions = false;

  instanceBackups: InstanceBackup[] = [];

  public goBack(): void {
    if (!this.hidePackDetails) {
      this.$router.push({ name: RouterNames.ROOT_LIBRARY });
    } else {
      this.searchingForMods = false;
      this.activeTab = ModpackPageTabs.OVERVIEW;
    }
  }

  public async launchModPack() {
    if (this.instance == null) {
      return;
    }

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
  }

  public confirmLaunch() {
    this.msgBox.type = 'okCancel';
    this.msgBox.title = 'Do you want to launch this modpack?';
    this.msgBox.okAction = this.launch;
    this.msgBox.cancelAction = this.hideMsgBox;
    this.msgBox.content = `We've been asked to launch ${this.instance?.name}, do you want to do this?`;
    this.showMsgBox = true;
  }

  public async launch() {
    await this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance?.uuid },
    });
  }

  public update(): void {
    const versionID = this.packInstance?.versions[0].id;

    this.installModpack({
      pack: {
        uuid: this.instance?.uuid,
        id: this.instance?.id,
        version: versionID,
        packType: this.instance?.packType,
      },
      meta: {
        name: this.instance?.name ?? '',
        version: this.packInstance?.versions[0].name ?? '',
        art: getPackArt(this.instance?.art),
      },
    });
    // if (this.modpacks?.installing !== null) {
    //   return;
    // }
    // const modpackID = this.instance?.id;
    // if (this.modpacks != null && this.packInstance != null) {
    //   if (versionID === undefined && this.packInstance.kind === 'modpack') {
    //     versionID = this.packInstance.versions[0].id;
    //   }
    //   this.$router.replace({
    //     name: RouterNames.ROOT_INSTALL_PACK,
    //     query: { modpackid: modpackID?.toString(), versionID: versionID?.toString(), uuid: this.instance?.uuid },
    //   });
    // }
  }

  public hideMsgBox(): void {
    this.showMsgBox = false;
  }

  async mounted() {
    if (this.instance == null) {
      await this.$router.push(RouterNames.ROOT_LIBRARY);
      return;
    }
    try {
      this.packInstance =
        this.instance.packType == 0
          ? await this.fetchModpack(this.instance.id).catch(() => undefined)
          : await this.fetchCursepack(this.instance.id).catch(() => undefined);
    } catch (e) {
      console.log('Error getting instance modpack');
    }
    this.$forceUpdate();

    if (this.$route.query.shouldPlay === 'true') {
      this.confirmLaunch();
    }

    this.getModList();
    this.loadBackups().catch(console.error);
  }

  public async loadBackups() {
    const backups = await wsTimeoutWrapperTyped<InstanceBackupsRequest, InstanceBackupsReply>({
      type: 'instanceGetBackups',
      uuid: this.instance?.uuid ?? '',
    });

    this.instanceBackups = backups.backups.sort((a, b) => b.createTime - a.createTime);
  }

  private getModList(showAlert = false) {
    try {
      this.updatingModlist = true;
      this.sendMessage({
        payload: {
          type: 'instanceMods',
          uuid: this.instance?.uuid,
        },
        callback: (data: any) => {
          this.modlist = data.files;
          this.updatingModlist = false;

          if (showAlert) {
            this.showAlert({
              title: 'Updated!',
              message: 'The mods list has been updated',
              type: 'primary',
            });
          }
        },
      });
      // I don't think we can catch this... Blame rush.
    } catch (e) {
      console.log(e);
      console.log('Error getting instance modlist');
    }
  }

  get instance() {
    if (this.modpacks == null) {
      return null;
    }
    return this.modpacks.installedPacks.filter((pack) => pack.uuid === this.$route.query.uuid)[0];
  }

  get isLatestVersion() {
    if (
      this.packInstance === undefined ||
      this.packInstance?.kind !== 'modpack' ||
      this.packInstance.versions === undefined
    ) {
      return true;
    }
    return this.instance?.versionId === this.packInstance?.versions[0].id;
  }

  get packSplashArt() {
    if (this.packInstance === null) {
      return PackConst.defaultPackSplashArt;
    }

    const splashArt = this.packInstance.art?.filter((art) => art.type === 'splash');
    return splashArt?.length > 0 ? splashArt[0].url : PackConst.defaultPackSplashArt;
  }

  get isForgePack() {
    return this.instance?.modLoader.includes('forge') ?? 'fabric';
  }

  /**
   * Determines if we need to hide the bulk of the page
   */
  get hidePackDetails() {
    return this.activeTab === ModpackPageTabs.SETTINGS;
  }

  get versionType() {
    return this.packInstance?.versions?.find((e) => e.id === this.instance?.versionId)?.type.toLowerCase() ?? 'release';
  }
}
</script>
