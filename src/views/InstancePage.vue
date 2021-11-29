<template>
  <div class="pack-page">
    <div
      class="pack-page-contents"
      v-if="instance && packInstance"
      :style="{
        'background-image': `url(${packSplashArt})`,
      }"
    >
      <header>
        <pack-meta-heading
          @back="goBack"
          :hidePackDetails="hidePackDetails"
          :versionType="versionType"
          :instance="instance"
          :isForgePack="isForgePack"
        />

        <pack-title-header v-if="!hidePackDetails" :pack-instance="packInstance" :pack-name="instance.name" />
      </header>

      <div class="body" v-if="!searchingForMods" :class="{ 'settings-open': activeTab === tabs.SETTINGS }">
        <pack-body
          v-if="!this.searchingForMods"
          @mainAction="launchModPack()"
          @update="update()"
          @tabChange="e => (activeTab = e)"
          @showVersion="showVersions = true"
          @searchForMods="searchingForMods = true"
          @getModList="e => getModList(e)"
          :searchingForMods="searchingForMods"
          :active-tab="activeTab"
          :isInstalled="true"
          :instance="instance"
          :mods="modlist"
          :pack-instance="packInstance"
          :isLatestVersion="isLatestVersion"
          :updating-mod-list="updatingModlist"
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

      <find-mods :instance="instance" @modInstalled="getModList" v-if="searchingForMods" />
    </div>

    <authentication v-if="authenticationOpen" @close="authenticationOpen = false" />

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
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModPack, ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import FTBModal from '@/components/FTBModal.vue';
import ServerCard from '@/components/ServerCard.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import { SettingsState } from '@/modules/settings/types';
import { ServersState } from '@/modules/servers/types';
// @ts-ignore
import placeholderImage from '@/assets/placeholder_art.png';
import { AuthState } from '@/modules/auth/types';
import FindMods from '@/components/modpack/FindMods.vue';
import { PackConst } from '@/utils/contants';
import ModpackVersions from '@/components/modpack/ModpackVersions.vue';
import ModpackPublicServers from '@/components/modpack/ModpackPublicServers.vue';
import ModpackSettings from '@/components/modpack/ModpackSettings.vue';
import PackMetaHeading from '@/components/modpack/modpack-elements/PackMetaHeading.vue';
import PackTitleHeader from '@/components/modpack/modpack-elements/PackTitleHeader.vue';
import PackBody from '@/components/modpack/modpack-elements/PackBody.vue';
import { App } from '@/types';
import { AuthProfile } from '@/modules/core/core.types';
import Authentication from '@/components/authentication/Authentication.vue';

export enum ModpackPageTabs {
  OVERVIEW,
  MODS,
  PUBLIC_SERVERS,
  SETTINGS,
}

@Component({
  name: 'InstancePage',
  components: {
    Authentication,
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

  authenticationOpen = false;

  public goBack(): void {
    if (!this.hidePackDetails) {
      this.$router.back();
    } else {
      this.searchingForMods = false;
      this.activeTab = ModpackPageTabs.MODS;
    }
  }

  public launchModPack() {
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
      this.launch();
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

  public async validateToken(profile?: AuthProfile) {
    console.log(profile);
    if (profile != null) {
      if (profile.type === 'microsoft') {
        // TODO: validate microsoft token
        // We need to store when the token expires using expires_in
      } else if (profile.type === 'mojang') {
          let rawResponse = await fetch(`https://authserver.mojang.com/validate`, {
            headers: {
              'Content-Type': 'application/json',
            },
            method: 'POST',
            body: JSON.stringify({ accessToken: profile.tokens.accessToken, clientToken: profile.tokens.clientToken }),
          });
          if (rawResponse.status === 204) {
            return false;
          } else {
            return true;
          }
      }
    } else {
      // TODO: There's no active profile
    }
  }

  public async refreshToken(profile?: AuthProfile) {
    if (profile != null) {
      if (profile.type === 'microsoft') {
        // TODO: refresh microsoft token
      } else if (profile.type === 'mojang') {
          let rawResponse = await fetch(`https://authserver.mojang.com/refresh`, {
            headers: {
              'Content-Type': 'application/json',
            },
            method: 'POST',
            body: JSON.stringify({ accessToken: profile.tokens.accessToken, clientToken: profile.tokens.clientToken }),
          });
          let response = await rawResponse.json();
          console.log(response.accessToken);
          // TODO: Handle when this doesn't work and ask for password again
          // TODO: update the tokens stored on the profile
      }
    } else {
      // TODO: There's no active profile
    }
  }

  public async launch(): Promise<void> {
    // TODO: REMOVE TRUE
    if (this.authProfiles.length === 0) {
      this.authenticationOpen = true;
      return;
    }

    // getActiveProfile data isn't the same as the mapped profiles
    let activeProfile = this.authProfiles.find((profile) => profile.uuid == this.getActiveProfile.uuid);

    const shouldRefresh = await this.validateToken(activeProfile);
    if (shouldRefresh) {
      console.log('We need to refresh');
      await this.refreshToken(activeProfile);
    }

    if (this.showMsgBox) {
      this.hideMsgBox();
    }

    const loadInApp =
      this.settingsState.settings.loadInApp === true ||
      this.settingsState.settings.loadInApp === 'true' ||
      this.auth.token?.activePlan == null;
    const disableChat = this.settingsState.settings.enableChat === true;
    this.sendMessage({
      payload: {
        type: 'launchInstance',
        loadInApp,
        uuid: this.instance?.uuid,
        extraArgs: disableChat ? '-Dmt.disablechat=true' : '',
      },
      callback: (data: any) => {},
    });
  }

  public update(versionID?: number): void {
    if (this.modpacks?.installing !== null) {
      return;
    }
    const modpackID = this.instance?.id;
    if (this.modpacks != null && this.packInstance != null) {
      if (versionID === undefined && this.packInstance.kind === 'modpack') {
        versionID = this.packInstance.versions[0].id;
      }
      this.$router.replace({
        name: 'installingpage',
        query: { modpackid: modpackID?.toString(), versionID: versionID?.toString(), uuid: this.instance?.uuid },
      });
    }
  }

  public hideMsgBox(): void {
    this.showMsgBox = false;
  }

  async mounted() {
    if (this.instance == null) {
      await this.$router.push('/modpacks');
      return;
    }
    try {
      this.packInstance =
        this.instance.packType == 0
          ? await this.fetchModpack(this.instance.id).catch((err: any) => undefined)
          : await this.fetchCursepack(this.instance.id).catch((err: any) => undefined);
    } catch (e) {
      console.log('Error getting instance modpack');
    }
    this.$forceUpdate();

    if (this.$route.query.shouldPlay === 'true') {
      this.confirmLaunch();
    }

    this.getModList();
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
    return this.modpacks.installedPacks.filter(pack => pack.uuid === this.$route.query.uuid)[0];
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

    const splashArt = this.packInstance.art?.filter(art => art.type === 'splash');
    return splashArt?.length > 0 ? splashArt[0].url : PackConst.defaultPackSplashArt;
  }

  get isForgePack() {
    return this.instance?.modLoader.includes('forge') ?? 'fabric';
  }

  /**
   * Determines if we need to hide the bulk of the page
   */
  get hidePackDetails() {
    return this.activeTab === ModpackPageTabs.SETTINGS || this.searchingForMods;
  }

  get versionType() {
    return this.packInstance?.versions?.find(e => e.id === this.instance?.versionId)?.type.toLowerCase() ?? 'release';
  }
}
</script>
