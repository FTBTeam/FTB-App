<template>
  <div class="pack-page">
    <div
      class="new-pack-page"
      v-if="instance && packInstance"
      :style="{
        'background-image': `url(${packSplashArt})`,
      }"
    >
      <header>
        <div class="meta-heading" :class="{ bolder: hidePackDetails }">
          <div class="back" @click="goBack">
            <font-awesome-icon icon="chevron-left" class="mr-2" />
            Back to {{ hidePackDetails ? 'instance mods' : 'library' }}
          </div>

          <div class="meta">
            <div class="origin icon" v-if="instance.packType === 0" data-balloon-pos="left" aria-label="FTB Modpack">
              <img src="@/assets/ftb-white-logo.svg" alt="" />
            </div>
            <div class="origin icon" v-else data-balloon-pos="left" aria-label="Curseforge Modpack">
              <img src="@/assets/curse-logo.svg" alt="" />
            </div>

            <div class="modloader icon" v-if="isForgePack" data-balloon-pos="left" aria-label="Forge Modloader">
              <img src="@/assets/images/forge.svg" alt="" />
            </div>
            <div class="modloader icon" v-else data-balloon-pos="left" aria-label="Fabric Modloader">
              <img src="@/assets/images/fabric.png" alt="" />
            </div>
          </div>
        </div>

        <div class="pack-info" v-if="!hidePackDetails">
          <div class="info">
            <div class="name">{{ instance.name }}</div>
            <div class="desc">
              {{ packInstance.name }}
              <template v-if="packInstance.authors.length"
                >by
                <span v-for="(author, i) in packInstance.authors" :key="'athrs' + i">{{ author.name }}</span></template
              >
              -
              {{ packInstance.synopsis }}
            </div>
          </div>
        </div>
      </header>

      <div class="body" :class="{ 'settings-open': activeTab === tabs.SETTINGS }">
        <div class="body-heading" v-if="!hidePackDetails">
          <div class="action-heading">
            <div class="play">
              <ftb-button color="primary" class="py-3 px-8" @click="launchModPack()">
                <font-awesome-icon icon="play" class="mr-4" />
                Play
              </ftb-button>
            </div>

            <div class="stats">
              <div class="stat">
                <div class="name">Last played</div>
                <div class="value">{{ instance.lastPlayed | moment('from', 'now') }}</div>
              </div>

              <div class="stat">
                <div class="name">Version</div>
                <div class="value">{{ instance.version }}</div>
              </div>
            </div>

            <div class="options">
              <div class="update" v-if="instance && !isLatestVersion">
                <ftb-button color="warning" class="update-btn px-4 py-1">
                  Update available
                  <font-awesome-icon icon="cloud-download-alt" class="ml-2" />
                </ftb-button>
              </div>
              <div class="option" @click="showVersions = true">
                Versions
                <font-awesome-icon icon="code-branch" class="ml-2" />
              </div>
              <div class="option" @click="activeTab = tabs.SETTINGS">
                Settings
                <font-awesome-icon icon="cogs" class="ml-2" />
              </div>
            </div>
          </div>

          <div class="tab-and-actions">
            <div class="tabs">
              <div class="options">
                <div
                  class="tab"
                  :class="{ active: activeTab === tabs.OVERVIEW }"
                  @click="() => (activeTab = tabs.OVERVIEW)"
                >
                  Overview
                </div>
                <div class="tab" :class="{ active: activeTab === tabs.MODS }" @click="() => (activeTab = tabs.MODS)">
                  Mods
                </div>
                <div
                  class="tab"
                  :class="{ active: activeTab === tabs.PUBLIC_SERVERS }"
                  @click="() => (activeTab = tabs.PUBLIC_SERVERS)"
                >
                  Public servers
                </div>
              </div>
            </div>

            <div class="actions">
              <div class="buttons" v-if="activeTab === tabs.MODS">
                <ftb-button color="primary" class="py-2 px-4 inline-block" @click="searchingForMods = true">
                  Get more mods
                  <font-awesome-icon icon="search" class="ml-2" />
                </ftb-button>
                <div class="refresh ml-4" aria-label="Refresh mod list" data-balloon-pos="down-right">
                  <ftb-button
                    @click="() => getModList(true)"
                    class="py-2 px-4"
                    color="info"
                    :disabled="updatingModlist"
                  >
                    <font-awesome-icon icon="sync" />
                  </ftb-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="body-contents">
          <div class="alert py-2 px-4 mb-4 bg-warning rounded" v-if="packInstance.notification">
            <font-awesome-icon icon="info" class="mr-2" />
            {{ packInstance.notification }}
          </div>

          <div class="pack-overview" v-if="activeTab === tabs.OVERVIEW">
            <div class="tags mb-6" v-if="tags.length">
              <span
                v-for="(tag, i) in tags"
                :key="`tag-${i}`"
                @click="clickTag(tag.name)"
                class="cursor-pointer rounded mr-2 text-sm ftb-tag px-4 py-1 lowercase"
                :style="{
                  fontVariant: 'small-caps',
                  backgroundColor: `hsla(${getColorForChar(tag.name)}, .5)`,
                }"
                >{{ tag.name }}</span
              >
            </div>
            <div class="" v-if="packInstance.description !== undefined">
              <VueShowdown :markdown="packInstance.description" :extensions="['classMap', 'attribMap', 'newLine']" />
            </div>
            <div v-else>
              <h2>No description available</h2>
            </div>
          </div>

          <!-- Tab views, we're not using the router because it's a pain -->
          <modpack-mods
            v-if="activeTab === tabs.MODS"
            v-show="!searchingForMods"
            :modlist="modlist"
            :instance="instance"
            @showFind="searchingForMods = true"
          />

          <!-- If the pack page grows more, we will have to use the router to clean this up. -->
          <modpack-settings :pack-instance="packInstance" :instance="instance" v-if="activeTab === tabs.SETTINGS" />

          <!-- v-show to allow servers to load in the background -->
          <modpack-public-servers
            v-show="activeTab === tabs.PUBLIC_SERVERS && currentVersionObject.mtgID"
            :instance="instance"
            :current-version="currentVersionObject.mtgID"
            :pack-instance="packInstance"
          />

          <find-mods
            :instance="instance"
            :goBack="() => (searchingForMods = false)"
            @modInstalled="getModList"
            v-if="searchingForMods"
          />
        </div>
      </div>

      <ftb-modal :isLarge="true" :visible="showVersions" @dismiss-modal="showVersions = false">
        <modpack-versions :versions="packInstance.versions" :current="instance.versionId" />
      </ftb-modal>
    </div>

    <div class="flex flex-1 flex-col h-full" v-if="false">
      <div class="flex flex-col h-full" v-if="instance != undefined" :key="instance.uuid">
        <div>
          <div class="header-image">
            <span class="instance-info" />
            <div class="instance-buttons flex flex-row frosted-glass">
              <div class="instance-button mr-1" v-if="instance && !isLatestVersion">
                <ftb-button
                  class="py-2 px-4"
                  color="warning"
                  @click="update()"
                  :disabled="modpacks.installing !== null"
                >
                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x" /> Update</span>
                </ftb-button>
              </div>

              <div class="instance-button mr-2 ml-auto">
                <ftb-button class="py-2 px-4" color="danger" css-class="text-center text-l" @click="confirmDelete()">
                  <font-awesome-icon icon="trash" size="1x" />
                  Delete
                </ftb-button>
              </div>
              <div class="instance-button mr-2">
                <ftb-button class="py-2 px-4" color="warning" css-class="text-center text-l" @click="browseInstance()">
                  <font-awesome-icon icon="folder" size="1x" />
                  Open Folder
                </ftb-button>
              </div>
            </div>
          </div>
        </div>

        <div style="height: auto; flex:1; overflow-y: auto;" class="flex flex-col">
          <div class="tab-pane" v-if="isTabActive('versions')" id="versions">
            <div v-for="(version, index) in packInstance.versions" :key="index">
              {{ debugLog('Version', version) }}
              <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                <p>{{ version.name }}</p>
                <span @click="toggleChangelog(version.id)" class="pl-5 cursor-pointer"
                  ><font-awesome-icon
                    :icon="activeChangelog === version.id ? 'chevron-down' : 'chevron-right'"
                    class="cursor-pointer"
                    size="1x"
                  />
                  Changelog</span
                >

                <ftb-button
                  v-if="instance.versionId && instance.versionId !== version.id"
                  class="py-2 px-4 ml-auto mr-1"
                  color="warning"
                  css-class="text-center text-l"
                  @click="update(version.id)"
                  :disabled="modpacks.installing !== null"
                >
                  <font-awesome-icon icon="download" size="1x" />
                  {{ isOlderVersion(version.id) ? 'Downgrade' : 'Update' }}
                </ftb-button>
                <!--                <button-->
                <!--                    v-if="instance.versionId && instance.versionId !== version.id"-->
                <!--                    class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-pointer"-->
                <!--                    @click="update(version.id)"-->
                <!--                >-->
                <!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> {{isOlderVersion(version.name) ? 'Downgrade' : 'Update'}}</span>-->
                <!--                </button>-->
                <ftb-button
                  v-if="instance.versionId && instance.versionId === version.id"
                  class="py-2 px-4 ml-auto mr-1 cursor-not-allowed"
                  color="primary"
                  css-class="text-center text-l cursor-not-allowed"
                >
                  <font-awesome-icon icon="check" size="1x" />
                  Current
                </ftb-button>
                <!--                <button-->
                <!--                    v-if="instance.versionId && instance.versionId === version.id"-->
                <!--                    class="bg-blue-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-not-allowed"-->
                <!--                >-->
                <!--                  <span class="cursor-not-allowed"><font-awesome-icon icon="check" size="1x"/> Current</span>-->
                <!--                </button>-->
                <v-selectmenu
                  :title="false"
                  :query="false"
                  :data="serverDownloadMenu(version.id)"
                  align="right"
                  type="regular"
                >
                  <ftb-button class="py-2 px-4 ml-2" color="info" css-class="text-center text-l">
                    <font-awesome-icon icon="download" size="1x" />
                    Download Server
                  </ftb-button>
                  <!--                  <button type="button" class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer"><span><font-awesome-icon icon="download" size="1x"/> Download Server</span></button>-->
                </v-selectmenu>
              </div>
              {{ debugLog('Version ID', version.id) }}
              {{ debugLog('activeChangelog', activeChangelog) }}
              <div class="pl-5 p-2 bg-background" v-if="activeChangelog === version.id">
                <VueShowdown
                  v-if="changelogs[version.id]"
                  :markdown="changelogs[version.id]"
                  :extensions="['classMap', 'newLine']"
                />
                <p v-else>No changelog available</p>
              </div>
            </div>
          </div>
          <div class="tab-pane" v-if="isTabActive('settings')" id="settings">
            <div class="bg-sidebar-item p-5 rounded my-4">
              <ftb-input label="Name" :value="instance.name" v-model="instance.name" @blur="saveSettings" />
              <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
                Window Size
              </label>
              <div class="flex flex-row my-4 -mt-2">
                <div class="flex-col mt-auto mb-2 pr-1">
                  <v-selectmenu
                    :title="false"
                    :query="false"
                    :data="resolutionList"
                    align="left"
                    :value="resSelectedValue"
                    @values="resChange"
                  >
                    <ftb-button class="py-2 px-4 my-1 w-full" color="primary" css-class="text-center text-l">
                      <font-awesome-icon icon="desktop" size="1x" />
                    </ftb-button>
                  </v-selectmenu>
                </div>

                <ftb-input
                  class="flex-col"
                  label="Width"
                  v-model="instance.width"
                  :value="instance.width"
                  @blur="saveSettings"
                />
                <font-awesome-icon class="mt-auto mb-6 mx-1 text-gray-600" icon="times" size="1x" />
                <ftb-input
                  class="flex-col"
                  label="Height"
                  v-model="instance.height"
                  :value="instance.height"
                  @blur="saveSettings"
                />
              </div>
              <div class="flex flex-col my-2">
                <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
                  Java Version
                </label>
                <select
                  class="rounded-none bg-input focus:outline-none focus:shadow-outline border border-input px-4 py-2 w-full h-full appearance-none leading-normal text-gray-400"
                  v-model="instance.jrePath"
                  @blur="saveSettings"
                  @change="saveSettings"
                >
                  <option
                    v-for="versionName in Object.keys(javaVersions)"
                    :value="javaVersions[versionName]"
                    :key="versionName"
                    >{{ versionName }}</option
                  >
                </select>
              </div>
            </div>
            <div class="flex flex-col my-2">
              <ftb-slider
                label="Instance Memory"
                v-model="instance.memory"
                :currentValue="instance.memory"
                minValue="512"
                :maxValue="settingsState.hardware.totalMemory"
                @blur="saveSettings"
                @change="saveSettings"
                step="64"
                unit="MB"
                css-class="memory"
                :raw-style="
                  `background: linear-gradient(to right, #8e0c25 ${(this.instance.minMemory /
                    settingsState.hardware.totalMemory) *
                    100 -
                    5}%, #a55805 ${(this.instance.minMemory / settingsState.hardware.totalMemory) *
                    100}%, #a55805 ${(this.instance.recMemory / settingsState.hardware.totalMemory) * 100 -
                    5}%, #005540 ${(this.instance.recMemory / settingsState.hardware.totalMemory) * 100}%);`
                "
              />
              <ftb-input label="Custom Arguments" v-model="instance.jvmArgs" @blur="saveSettings" />
              <ftb-toggle
                label="Enable cloud save uploads"
                :disabled="
                  auth.token !== undefined &&
                    auth.token !== null &&
                    auth.token.activePlan !== undefined &&
                    auth.token.activePlan !== null &&
                    settingsState.settings.cloudSaves !== true &&
                    settingsState.settings.cloudSaves !== 'true'
                "
                onColor="bg-primary"
                inline="true"
                :value="instance.cloudSaves"
                @change="toggleCloudSaves"
              />
              <ftb-button class="py-2 px-4 w-10 ml-auto mr-2" color="primary" css-class="text-center text-l">
                <font-awesome-icon icon="save" size="1x" />&nbsp;Save
              </ftb-button>
              <!--              <button-->
              <!--                  class="cursor-pointer bg-green-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto mr-2 mb-2"-->
              <!--              >-->
              <!--                <font-awesome-icon icon="save" size="1x"/>&nbsp;Save-->
              <!--              </button>-->
            </div>
          </div>
        </div>
        <div class="tab-content bg-navbar flex-1 py-4 px-4 mx-2" style="overflow-y: auto;">
          <find-mods :instance="instance" :goBack="() => (searchingForMods = false)" @modInstalled="getModList" />
        </div>
      </div>
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
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModPack, ModpackState, Versions } from '@/modules/modpacks/types';
import { Action, State } from 'vuex-class';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import FTBModal from '@/components/FTBModal.vue';
import ServerCard from '@/components/ServerCard.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import { SettingsState } from '@/modules/settings/types';
import { logVerbose } from '@/utils';
import { ServersState } from '@/modules/servers/types';
// @ts-ignore
import placeholderImage from '@/assets/placeholder_art.png';
import { AuthState } from '@/modules/auth/types';
import platform from '@/utils/interface/electron-overwolf';
import { prettyByteFormat } from '@/utils/helpers';
import FindMods from '@/components/modpack/FindMods.vue';
import { PackConst } from '@/utils/contants';
import ModpackVersions from '@/components/modpack/ModpackVersions.vue';
import ModpackPublicServers from '@/components/modpack/ModpackPublicServers.vue';
import ModpackMods from '@/components/modpack/ModpackMods.vue';
import ModpackSettings from '@/components/modpack/ModpackSettings.vue';
import { getColorForChar } from '@/utils/colors';

interface MsgBox {
  title: string;
  content: string;
  type: string;
  okAction: () => void;
  cancelAction: () => void;
}

interface Changelogs {
  [id: number]: string;
}

enum Tabs {
  OVERVIEW,
  MODS,
  PUBLIC_SERVERS,
  SETTINGS,
}

@Component({
  name: 'InstancePage',
  components: {
    ModpackSettings,
    ServerCard,
    'ftb-modal': FTBModal,
    'ftb-input': FTBInput,
    'ftb-toggle': FTBToggle,
    'ftb-slider': FTBSlider,
    'ftb-button': FTBButton,
    ModpackVersions,
    MessageModal,
    FindMods,
    ModpackMods,
    ModpackPublicServers,
  },
})
export default class InstancePage extends Vue {
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @State('settings') public settingsState!: SettingsState;
  @State('servers') public serverListState!: ServersState;
  @State('auth') public auth!: AuthState;
  @Action('fetchServers', { namespace: 'servers' }) public fetchServers!: (projectid: string) => void;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: any;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall!: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall!: any;
  @Action('saveInstance', { namespace: 'modpacks' }) public saveInstance: any;
  @Action('sendMessage') public sendMessage!: any;
  @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;
  @Action('loadJavaVersions', { namespace: 'settings' }) public loadJavaVersions!: any;
  @Action('showAlert') public showAlert: any;

  // New stuff
  tabs = Tabs;
  activeTab: Tabs = Tabs.OVERVIEW;
  getColorForChar = getColorForChar;

  private packInstance: ModPack | null = null;
  deleting: boolean = false;

  private showMsgBox: boolean = false;
  private msgBox: MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };

  private activeChangelog: number | undefined = -1;
  private changelogs: Changelogs = [];
  private modlist: any = [];

  private resSelectedValue: string = '0';
  prettyBytes = prettyByteFormat;

  searchingForMods = false;
  updatingModlist = false;
  showVersions = false;

  public clickTag(tagName: string) {
    this.$router.push({ name: 'browseModpacks', params: { search: tagName } });
  }

  public serverDownloadMenu(versionID: number) {
    const links = [];
    links.push({
      content: 'Windows',
      url: `${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.instance?.id}/${versionID}/server/windows`,
      open: '_blank',
    });
    links.push({
      content: 'Linux',
      url: `${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.instance?.id}/${versionID}/server/linux`,
      open: '_blank',
    });
    links.push({
      content: 'MacOS',
      url: `${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.instance?.id}/${versionID}/server/mac`,
      open: '_blank',
    });
    return links;
  }

  public goBack(): void {
    if (!this.hidePackDetails) {
      this.$router.back();
    } else {
      this.searchingForMods = false;
      this.activeTab = Tabs.OVERVIEW;
    }
  }

  public copy(text: string) {
    platform.get.cb.copy(text);
    this.showAlert({
      title: 'Copied!',
      message: 'The value has been copied to your clipboard',
      type: 'primary',
    });
  }

  public isTabActive(tabItem: string) {
    return true;
  }

  public setActiveTab(tabItem: string) {}

  public isOlderVersion(version: number) {
    if (this.instance == null) {
      return false;
    }
    return this.instance?.versionId > version;
  }

  public isCurrentVersion(version: number) {
    return this.instance?.versionId && this.instance?.versionId === version;
  }

  public toggleCloudSaves() {
    if (this.instance) {
      this.instance.cloudSaves = !this.instance.cloudSaves;
    }
  }

  public confirmDelete() {
    this.openMessageBox({
      type: 'okCancel',
      title: 'Are you sure?',
      okAction: this.deleteInstace,
      cancelAction: this.hideMsgBox,
      content: `Are you sure you want to delete ${this.instance?.name}?`,
    });
  }

  public deleteInstace(): void {
    this.deleting = true;
    this.sendMessage({
      payload: { type: 'uninstallInstance', uuid: this.instance?.uuid },
      callback: (data: any) => {
        this.sendMessage({
          payload: { type: 'installedInstances', refresh: true },
          callback: (data: any) => {
            this.storePacks(data);
            this.$router.push({ name: 'modpacks' });
          },
        });
      },
    });
  }

  private openMessageBox(payload: MsgBox) {
    this.msgBox = { ...this.msgBox, ...payload };
    this.showMsgBox = true;
  }

  public browseInstance(): void {
    this.sendMessage({
      payload: { type: 'instanceBrowse', uuid: this.instance?.uuid },
      callback: (data: any) => {},
    });
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

  public launch(): void {
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

  public async saveSettings() {
    await this.saveInstance(this.instance);
    this.showAlert({
      title: 'Saved!',
      message: 'The settings for this instance have been saved',
      type: 'primary',
    });
  }

  public resChange(data: any) {
    if (data && data.length) {
      if (this.resSelectedValue === data[0].value) {
        return;
      }
      this.resSelectedValue = data[0].value;
      if (this.instance == null) {
        return;
      }
      this.instance.width = this.settingsState.hardware.supportedResolutions[data[0].id].width;
      this.instance.height = this.settingsState.hardware.supportedResolutions[data[0].id].height;

      this.saveSettings();
      return;
    }
  }

  public hideMsgBox(): void {
    this.showMsgBox = false;
  }

  async mounted() {
    if (this.instance == null) {
      this.$router.push('/modpacks');
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
    if (this.packInstance?.kind === 'modpack') {
      if (this.packInstance?.versions !== undefined) {
        this.toggleChangelog(this.packInstance?.versions[0].id);
      }
    }
    if (this.$route.query.shouldPlay === 'true') {
      this.confirmLaunch();
    }

    this.getModList();

    if (Object.keys(this.settingsState.javaInstalls).length < 1) {
      this.loadJavaVersions();
    }
  }

  get currentVersionObject(): Versions | null {
    if (this.packInstance !== null) {
      if (this.packInstance.versions === undefined) {
        return null;
      }
      const version = this.packInstance.versions.find((f: Versions) => f.id === this.instance?.versionId);
      if (version !== undefined) {
        return version;
      }
    }
    return null;
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

  private async toggleChangelog(id: number | undefined) {
    if (typeof id === 'undefined') {
      return;
    }
    if (this.activeChangelog === id) {
      this.activeChangelog = -1;
      return;
    }
    if (!this.changelogs[id]) {
      console.log('Getting changelog');
      try {
        const changelog = await this.getChangelog({
          packID: this.instance?.id,
          versionID: id,
          type: this.instance?.packType,
        });
        this.changelogs[id] = changelog.content;
      } catch (e) {
        console.log("Changelog doesn't exist");
      }
    }
    this.activeChangelog = id;
  }

  private debugLog(title: any, data: any) {
    logVerbose(this.settingsState, title, data);
  }

  get javaVersions() {
    if (this.settingsState === undefined) {
      return {};
    }
    if (this.settingsState.javaInstalls === undefined) {
      return {};
    }
    return this.settingsState.javaInstalls;
  }

  get instance() {
    if (this.modpacks == null) {
      return null;
    }
    return this.modpacks.installedPacks.filter(pack => pack.uuid === this.$route.query.uuid)[0];
  }

  get resolutionList() {
    const resList = [];
    for (const [key, res] of Object.entries(this.settingsState.hardware.supportedResolutions)) {
      resList.push({ id: key, name: res.width + 'x' + res.height, value: key });
    }
    return resList;
  }

  get tags() {
    try {
      if (this.instance && this.modpacks?.packsCache[this.instance.id]) {
        return this.modpacks?.packsCache[this.instance.id].tags?.sort((a, b) =>
          a.name < b.name ? -1 : a.name > b.name ? 1 : 0,
        );
      } else {
        return [];
      }
    } catch (e) {
      console.log('Error getting tags');
    }
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

  get versionName() {
    if (this.instance && this.modpacks?.packsCache[this.instance.id]) {
      return this.modpacks?.packsCache[this.instance.id].versions.find(v => v.id === this.instance?.versionId)?.name;
    }
    return this.instance?.version;
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
    return this.activeTab === Tabs.SETTINGS || this.searchingForMods;
  }
}
</script>

<style lang="scss" scoped>
.pack-page {
  position: relative;
  z-index: 1;
}

.new-pack-page {
  position: relative;
  background-attachment: fixed;
  min-height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;

  > header {
    position: relative;
    backdrop-filter: blur(5px);

    .meta-heading {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #2a2a2a;
      padding: 0.8rem 1rem;
      position: relative;
      z-index: 1;
      transition: background-color 0.25s ease-in-out;

      &.bolder {
        background-color: var(--color-navbar);

        .meta {
          opacity: 0;
        }
      }

      .back {
        opacity: 0.7;
        cursor: pointer;
        transition: opacity 0.25s ease-in-out;

        &:hover {
          opacity: 1;
        }
      }

      .meta {
        display: flex;
        align-items: center;
        transition: opacity 0.25s ease-in-out;

        .icon {
          cursor: default;
          margin-left: 1.5rem;
          img {
            width: 30px;
          }
        }
      }
    }

    .pack-info {
      padding: 2rem 1rem;
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      text-shadow: 0 0 5px rgba(black, 0.5);
      background-color: rgba(black, 0.2);

      .info {
        max-width: 80%;
        padding: 1rem 4rem;
      }

      .name {
        font-size: 2.8rem;
        font-weight: bold;
      }
    }
  }

  .body {
    flex: 1;
    background: rgba(#2a2a2a, 0.9);
    backdrop-filter: blur(8px);
    transition: background-color 0.25s ease-in-out;

    .ftb-tag {
      background-color: rgba(white, 0.2);
    }

    &.settings-open {
      background-color: var(--color-background);
    }

    .action-heading {
      padding: 1rem 1.5rem;
      display: flex;
      align-items: center;
      background-color: rgba(black, 0.1);

      .play {
        margin-right: 2rem;
      }

      .stats {
        display: flex;

        .stat {
          margin-right: 2rem;

          .name {
            opacity: 0.7;
            font-size: 0.875rem;
          }
        }
      }

      .options {
        display: flex;
        align-items: center;
        margin-left: auto;

        .option {
          margin-left: 2rem;
          opacity: 0.7;
          cursor: pointer;
          transition: opacity 0.25s ease-in-out;

          &:hover {
            opacity: 1;
          }
        }
      }

      .update-btn {
        position: relative;

        box-shadow: 0 0 0 0 rgba(#ff801e, 1);
        animation: pulse 1.8s ease-in-out infinite;

        @keyframes pulse {
          0% {
            box-shadow: 0 0 0 0 rgba(#ff801e, 0.7);
          }

          70% {
            box-shadow: 0 0 0 10px rgba(#ff801e, 0);
          }

          100% {
            box-shadow: 0 0 0 0 rgba(#ff801e, 0);
          }
        }
      }
    }

    .tab-and-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(black, 0.3);
      padding-right: 1.5rem;
      margin-bottom: 0.8rem;

      .actions {
        .buttons {
          display: flex;
        }
      }

      .tabs {
        display: flex;
        align-items: center;
        padding: 0.5rem 1.5rem 0 1.5rem;

        .options {
          display: flex;
        }

        .tab {
          padding: 0.5rem 1.5rem 1rem 1.5rem;
          color: rgba(white, 0.5);
          cursor: pointer;
          transition: color 0.25s ease-in-out, background-color 0.25s ease-in-out;
          margin-right: 1rem;
          border-bottom: 2px solid transparent;

          &.active {
            border-bottom-color: var(--color-info-button);
            color: white;
          }

          &:hover {
            color: white;
          }
        }
      }
    }

    .body-contents {
      padding: 0.8rem 1.5rem 1rem 1.5rem;
    }
  }
}
</style>
