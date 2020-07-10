<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div class="flex flex-col h-full" v-if="instance != undefined " :key="instance.uuid">
      <div>
        <div
            class="header-image"
            v-bind:style="{'background-image': `url(${currentModpack !== null && typeof currentModpack.art === 'Array' && currentModpack.art.filter((art) => art.type === 'splash').length > 0 ? currentModpack.art.filter((art) => art.type === 'splash')[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'})`}"
        >
          <span class="instance-name text-4xl pb-2">{{instance.name}}</span>
          <span class="instance-info">
            <small v-if="currentModpack !== null">
              {{currentModpack.name}} <span v-for="author in currentModpack.authors">By {{author.name}}</span> - {{instance.version}} -
              <em>{{currentModpack.synopsis}}</em>
            </small>
            <div v-if="currentModpack !== null && tags.length > 0" class="flex flex-row items-center">
                <div class="flex flex-row">
                    <span v-for="(tag, i) in limitedTags" :key="`tag-${i}`" @click="clickTag(tag.name)" class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
                    <span v-if="tags.length > 5" :key="`tag-more`" class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+{{tags.length - 5}}</span>
                </div>
            </div>
          </span>
          <div class="update-bar"  v-if="instance && !isLatestVersion">
            A new update is available
          </div>
          <div class="update-bar"  v-if="currentModpack && currentModpack.notification">
            {{currentModpack.notification}}
          </div>
          <div class="instance-buttons flex flex-row frosted-glass">
            <div class="instance-button mr-1">
              <ftb-button class="py-2 px-4" color="primary" css-class="text-center text-l" @click="checkMemory()"><font-awesome-icon icon="play" size="1x"/> Play</ftb-button>
<!--              <button-->
<!--                  class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"-->
<!--                  @click="checkMemory()"-->
<!--              >-->
<!--                <span class="cursor-pointer"><font-awesome-icon icon="play" size="1x"/> Play</span>-->
<!--              </button>-->
            </div>
            <div class="instance-button mr-1" v-if="instance && !isLatestVersion">
              <ftb-button
                  class="py-2 px-4" color="warning"
                  @click="update()"
              >
                <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> Update</span>
              </ftb-button>
            </div>
            <div class="instance-button mr-1" title="Last played">
              <div class="text-white-500 py-2 px-4 inline-flex items-center">
                <font-awesome-icon icon="stopwatch" size="1x"/>&nbsp;
                <small
                    class="ml-2 text-gray-400 "
                >{{instance.lastPlayed | moment("from", "now")}}</small>
              </div>
            </div>
            <div class="instance-button mr-2 ml-auto">
              <ftb-button class="py-2 px-4" color="danger" css-class="text-center text-l" @click="confirmDelete()"><font-awesome-icon icon="trash" size="1x"/> Delete</ftb-button>
<!--              <button-->
<!--                  class="bg-red-700 hover:bg-red-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"-->
<!--                  @click="confirmDelete()"-->
<!--              >-->
<!--                <span class="cursor-pointer"><font-awesome-icon icon="trash" size="1x"/> Delete</span>-->
<!--              </button>-->
            </div>
            <div class="instance-button mr-2">
              <ftb-button class="py-2 px-4" color="warning" css-class="text-center text-l" @click="browseInstance()"><font-awesome-icon icon="folder" size="1x"/> Open Folder</ftb-button>
<!--              <button-->
<!--                  class="bg-orange-700 hover:bg-orange-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"-->
<!--                  @click="browseInstance()"-->
<!--              >-->
<!--                <span class="cursor-pointer"><font-awesome-icon icon="folder" size="1x"/> Open Folder</span>-->
<!--              </button>-->
            </div>
          </div>
        </div>
      </div>
      <div style="height: auto; flex:1; overflow-y: auto;" class="flex flex-col">
        <ul class="flex p-2 pb-0" style="position: sticky; top: 0; background: #2A2A2A; padding-bottom: 0;">
          <li class="-mb-px mr-1">
            <a
                class="bg-sidebar-item inline-block p-2 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('overview')"
                :class="{ 'border-l border-t border-r border-navbar': isTabActive('overview'), 'text-gray-600 hover:text-gray-500': !isTabActive('overview') }"
                href="#overview"
            >Overview</a>
          </li>
          <li class="-mb-px mr-1" v-if="currentModpack != undefined && currentModpack.versions !== undefined">
            <a
                class="bg-sidebar-item inline-block p-2 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('versions')"
                :class="{ 'border-l border-t border-r border-navbar': isTabActive('versions'), 'text-gray-600 hover:text-gray-500': !isTabActive('versions') }"
                href="#versions"
            >Versions</a>
          </li>
          <li class="-mb-px mr-1">
            <a
                    class="bg-sidebar-item inline-block p-2 font-semibold cursor-pointer"
                    @click.prevent="setActiveTab('modlist')"
                    :class="{ 'border-l border-t border-r border-navbar': isTabActive('modlist'), 'text-gray-600 hover:text-gray-500': !isTabActive('modlist') }"
                    href="#versions"
            >ModList</a>
          </li>
          <li class="-mb-px mr-1">
            <a
                class="bg-sidebar-item inline-block p-2 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('settings')"
                :class="{ 'border-l border-t border-r border-navbar': isTabActive('settings'), 'text-gray-600 hover:text-gray-500': !isTabActive('settings') }"
                href="#settings"
            >Settings</a>
          </li>
        </ul>
        <div class="tab-content bg-navbar flex-1 p-2 py-4 mx-2" style="overflow-y: auto;">
          <div class="tab-pane" v-if="isTabActive('overview')" id="overview">
            <div class="flex flex-wrap" v-if="currentModpack != undefined && currentModpack.description !== undefined">
              <VueShowdown :markdown="currentModpack.description" :extensions="['classMap', 'attribMap', 'newLine']"/>
            </div>
            <div v-else>
              <h2>No description available</h2>
            </div>
          </div>
          <div class="tab-pane" v-if="isTabActive('versions')" id="versions">
            <div v-for="(version, index) in currentModpack.versions" :key="index">
              {{debugLog("Version", version)}}
              <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                <p>{{currentModpack.name}} - {{version.name}}</p>
                <span @click="toggleChangelog(version.id)" class="pl-5 cursor-pointer"><font-awesome-icon
                    :icon="activeChangelog === version.id ? 'chevron-down' : 'chevron-right'" class="cursor-pointer"
                    size="1x"/> Changelog</span>

                <ftb-button v-if="instance.versionId && instance.versionId !== version.id" class="py-2 px-4 ml-auto mr-1" color="warning" css-class="text-center text-l" @click="update(version.id)"><font-awesome-icon icon="download" size="1x"/> {{isOlderVersion(version.name) ? 'Downgrade' : 'Update'}}</ftb-button>
<!--                <button-->
<!--                    v-if="instance.versionId && instance.versionId !== version.id"-->
<!--                    class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-pointer"-->
<!--                    @click="update(version.id)"-->
<!--                >-->
<!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> {{isOlderVersion(version.name) ? 'Downgrade' : 'Update'}}</span>-->
<!--                </button>-->
                <ftb-button v-if="instance.versionId && instance.versionId === version.id" class="py-2 px-4 ml-auto mr-1 cursor-not-allowed" color="primary" css-class="text-center text-l cursor-not-allowed"><font-awesome-icon icon="check" size="1x"/> Current</ftb-button>
<!--                <button-->
<!--                    v-if="instance.versionId && instance.versionId === version.id"-->
<!--                    class="bg-blue-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-not-allowed"-->
<!--                >-->
<!--                  <span class="cursor-not-allowed"><font-awesome-icon icon="check" size="1x"/> Current</span>-->
<!--                </button>-->
                <v-selectmenu :title="false" :query="false" :data="serverDownloadMenu(version.id)" align="right" type="regular">
                  <ftb-button class="py-2 px-4 ml-2" color="info" css-class="text-center text-l"><font-awesome-icon icon="download" size="1x"/> Download Server</ftb-button>
<!--                  <button type="button" class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer"><span><font-awesome-icon icon="download" size="1x"/> Download Server</span></button>-->
                </v-selectmenu>
              </div>
              {{debugLog("Version ID", version.id)}}
              {{debugLog("activeChangelog", activeChangelog)}}
              <div class="pl-5 p-2 bg-background" v-if="activeChangelog === version.id">
                <VueShowdown v-if="changelogs[version.id]" :markdown="changelogs[version.id]" :extensions="['classMap', 'newLine']"/>
                <p v-else>No changelog available</p>
              </div>
            </div>
          </div>
          <div class="tab-pane" v-if="isTabActive('settings')" id="settings">
            <div class="bg-sidebar-item p-5 rounded my-4">
              <ftb-input
                  label="Name"
                  :value="instance.name"
                  v-model="instance.name"
                  @blur="saveSettings"
              />
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
                    <ftb-button class="py-2 px-4 my-1 w-full" color="primary" css-class="text-center text-l"><font-awesome-icon icon="desktop" size="1x"/></ftb-button>
                  </v-selectmenu>
                </div>

                <ftb-input
                    class="flex-col"
                    label="Width"
                    v-model="instance.width"
                    :value="instance.width"
                    @blur="saveSettings"
                />
                <font-awesome-icon class="mt-auto mb-6 mx-1 text-gray-600" icon="times" size="1x"/>
                <ftb-input
                    class="flex-col"
                    label="Height"
                    v-model="instance.height"
                    :value="instance.height"
                    @blur="saveSettings"
                />
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
                  :raw-style="`background: linear-gradient(to right, #8e0c25 ${((this.instance.minMemory / settingsState.hardware.totalMemory) * 100) - 5}%, #a55805 ${((this.instance.minMemory / settingsState.hardware.totalMemory) * 100)}%, #a55805 ${((this.instance.recMemory / settingsState.hardware.totalMemory) * 100) - 5}%, #005540 ${((this.instance.recMemory / settingsState.hardware.totalMemory) * 100)}%);`"
              />
              <ftb-input label="Custom Arguments" v-model="instance.jvmArgs" @blur="saveSettings"/>
              <ftb-button class="py-2 px-4 w-10 ml-auto mr-2" color="primary" css-class="text-center text-l"><font-awesome-icon icon="save" size="1x"/>&nbsp;Save</ftb-button>
<!--              <button-->
<!--                  class="cursor-pointer bg-green-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto mr-2 mb-2"-->
<!--              >-->
<!--                <font-awesome-icon icon="save" size="1x"/>&nbsp;Save-->
<!--              </button>-->
            </div>
          </div>

          <div class="tab-pane" v-if="isTabActive('modlist')" id="modlist">
            <div v-for="file in modlist">
              <div class="flex flex-row bg-background p-5 my-4 items-center">
                <p :title="`Version ${file.version}`">{{file.name.replace('.jar', '')}}</p>
                <div class="ml-auto">
                  <span class="rounded mx-2 text-sm bg-gray-600 py-1 px-2">{{parseInt(file.size) | prettyBytes}}</span>
                  <v-selectmenu :title="false" :query="false" :data="modHashData(file)" align="right" type="regular">
                    <template #row="{ row }">
                      <font-awesome-icon :color="row.color" :icon="row.icon"></font-awesome-icon>
                      <span v-html="row.content" :title="row.hover" class="ml-2 cursor-pointer"></span>
                    </template>
                  <ftb-button class="py-2 px-4 ml-2" color="info" css-class="text-center text-l"><font-awesome-icon icon="shield-alt"></font-awesome-icon></ftb-button>
<!--                  <button type="button" class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer"><span><font-awesome-icon icon="download" size="1x"/> Download Server</span></button>-->
                  </v-selectmenu>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <FTBModal :visible="showMsgBox" @dismiss-modal="hideMsgBox">
      <message-modal :title="msgBox.title" :content="msgBox.content" :ok-action="msgBox.okAction"
                     :cancel-action="msgBox.cancelAction" :type="msgBox.type"/>
    </FTBModal>
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

  @media screen and (max-height: 800px) {
    .header-image {
      transition: all 0.2s ease-in-out;
    }
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

  .update-bar {
    background: rgba(	255, 193, 7, 0.9);
    width: 100%;
    height: 25px;
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
   .frosted-glass {
      backdrop-filter: blur(8px);
      background: linear-gradient(to top, rgba(36, 40, 47, 0) 0%, rgba(43, 57, 66, 0.2) calc( 100% - 2px), rgba(193, 202, 207, 0.1) calc( 100% - 1px), rgba(29, 29, 29, 0.3) 100%);
    //   -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,1) 50%, rgba(0,0,0,0) 100%);
  }
</style>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {ModpackState, ModPack, Instance, Art, Authors, Versions} from '@/modules/modpacks/types';
import {State, Action} from 'vuex-class';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import FTBModal from '@/components/FTBModal.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import * as semver from 'semver';
import config from '@/config';
import moment from 'moment';
import {SettingsState} from '@/modules/settings/types';
import { clipboard} from 'electron';
import fs from 'fs';
import { logVerbose } from '../utils';

interface MsgBox {
    title: string;
    content: string;
    type: string;
    okAction: () => void;
    cancelAction: () => void;
}

interface Modlist {
  name: string;
  version: string;
  type: string;
  size: string;
  updated: string;
  sha1: string;
}

interface Changelogs {
    [id: number]: string;
}

@Component({
    name: 'InstancePage',
    components: {
        FTBModal,
        'ftb-input': FTBInput,
        'ftb-toggle': FTBToggle,
        'ftb-slider': FTBSlider,
        'ftb-button': FTBButton,
        'message-modal': MessageModal,
    },
})

export default class InstancePage extends Vue {

    get instance() {
        if (this.modpacks == null) {
            return null;
        }
        return this.modpacks.installedPacks.filter(
            (pack) => pack.uuid === this.$route.query.uuid,
        )[0];
    }

    get resolutionList() {
        const resList = [];
        for (const [key, res] of Object.entries(
            this.settingsState.hardware.supportedResolutions,
        )) {
            resList.push({id: key, name: res.width + 'x' + res.height, value: key});
        }
        return resList;
    }

    get tags() {
       if (this.instance && this.modpacks?.packsCache[this.instance.id]) {
            return this.modpacks?.packsCache[this.instance.id].tags;
        } else {
            return [];
        }
    }

    get limitedTags() {
      if (this.tags) {
        return this.tags.slice(0, 5);
      }
      return [];
    }

    get isLatestVersion() {
      if (this.currentModpack === undefined || this.currentModpack?.kind !== 'modpack') {
        return true;
      }
      return this.instance?.versionId === this.currentModpack?.versions[0].id;
    }


    @State('modpacks') public modpacks: ModpackState | undefined = undefined;
    @State('settings') public settingsState!: SettingsState;
    @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack!: any;
    @Action('storeInstalledPacks', {namespace: 'modpacks'})
    public storePacks!: any;
    @Action('updateInstall', {namespace: 'modpacks'})
    public updateInstall!: any;
    @Action('finishInstall', {namespace: 'modpacks'})
    public finishInstall!: any;
    @Action('saveInstance', {namespace: 'modpacks'}) public saveInstance: any;
    @Action('sendMessage') public sendMessage!: any;
    @Action('getChangelog', {namespace: 'modpacks'}) public getChangelog!: any;
    @Action('showAlert') public showAlert: any;
    @Action('hideAlert') public hideAlert: any;

    private currentModpack: ModPack | null = null;
    private cheaty: string = '';

    private activeTab: string = 'overview';
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

    public clickTag(tagName: string) {
        this.$router.push({name: 'browseModpacks', params: {search: tagName}});
    }

    public serverDownloadMenu(versionID: number) {
        const links = [];
        links.push({content: 'Windows', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/windows`, open: '_blank'});
        links.push({content: 'Linux', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/linux`, open: '_blank'});
        links.push({content: 'MacOS', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/mac`, open: '_blank'});
        return links;
    }

    public copy(text: string) {
      clipboard.writeText(text);
      this.showAlert({
          title: 'Copied!',
          message: 'The value has been copied to your clipboard',
          type: 'primary',
      });
      setTimeout(() => {
          this.hideAlert();
      }, 5000);
    }

    public modHashData(file: any) {
        const links = [];
        const isMatched = true;
        links.push({icon: 'check-circle', color: isMatched ? 'green' : 'red', content: `SHA1`, callback: this.copy.bind(this, file.sha1)});
        links.push({icon: 'times-circle', color:  'red',  hover: 'Coming Soon', content: 'MD5'});
        links.push({icon: 'times-circle', color:  'red',  hover: 'Coming Soon', content: 'SHA256'});
        return links;
    }

    public isTabActive(tabItem: string) {
        return this.activeTab === tabItem;
    }

    public setActiveTab(tabItem: string) {
        this.activeTab = tabItem;
    }

    public isOlderVersion(version: string) {
        if (this.instance == null) {
            return false;
        }
        return semver.lt(version, this.instance.version);
    }

    public isCurrentVersion(version: number) {
        return this.instance?.versionId && this.instance?.versionId === version;
    }

    public confirmDelete() {
        this.msgBox.type = 'okCancel';
        this.msgBox.title = 'Are you sure?';
        this.msgBox.okAction = this.deleteInstace;
        this.msgBox.cancelAction = this.hideMsgBox;
        this.msgBox.content = `Are you sure you want to delete ${this.instance?.name}?`;
        this.showMsgBox = true;
    }

    public deleteInstace(): void {
        this.sendMessage({
            payload: {type: 'uninstallInstance', uuid: this.instance?.uuid},
            callback: (data: any) => {
                this.sendMessage({
                    payload: {type: 'installedInstances'},
                    callback: (data: any) => {
                        this.storePacks(data);
                        this.$router.push({name: 'modpacks'});
                    },
                });
            },
        });
    }

    public comingSoonMsg() {
        this.msgBox.type = 'okOnly';
        this.msgBox.title = 'Coming Soon';
        this.msgBox.okAction = this.hideMsgBox;
        this.msgBox.cancelAction = this.hideMsgBox;
        this.msgBox.content = `This feature is currently not available, we do however aim to have this feature implemented in the near future`;
        this.showMsgBox = true;
    }
    public downloadServer() {
        this.msgBox.type = 'okOnly';
        this.msgBox.title = 'Server Downloads';
        this.msgBox.okAction = this.hideMsgBox;
        this.msgBox.cancelAction = this.hideMsgBox;
        this.msgBox.content = `To download the server files for this modpack please go to the [Feed-the-Beast](https://feed-the-beast.com/) website`;
        this.showMsgBox = true;
    }

    public browseInstance(): void {
        this.sendMessage({
            payload: {type: 'instanceBrowse', uuid: this.instance?.uuid},
            callback: (data: any) => {
            },
        });
    }

    public checkMemory() {
        if (this.instance == null) {
            return;
        }
        if (this.instance.memory < this.instance.minMemory) {
            this.msgBox.type = 'okCancel';
            this.msgBox.title = 'Low Memory';
            this.msgBox.okAction = this.launch;
            this.msgBox.cancelAction = this.hideMsgBox;
            this.msgBox.content = `You are trying to launch the modpack with memory settings that are below the` +
            `minimum required.This may cause the modpack to not start or crash frequently.<br>We recommend that you` +
            `increase the assigned memory to at least **${this.instance?.minMemory}MB**\n\nYou can change the memory by going to the settings tab of the modpack and adjusting the memory slider`;
            this.showMsgBox = true;
        } else {
            this.launch();
        }
    }

    public launch(): void {
        this.sendMessage({
            payload: {type: 'launchInstance', uuid: this.instance?.uuid},
            callback: (data: any) => {
            },
        });
    }

    public update(versionID?: number): void {
        const modpackID = this.instance?.id;
        this.updateInstall({modpackID: this.instance?.id, progress: 0});
        if (this.modpacks != null && this.currentModpack != null) {
            if (versionID === undefined && this.currentModpack.kind === 'modpack') {
                versionID = this.currentModpack.versions[0].id;
            }
            this.sendMessage({
                payload: {
                    type: 'updateInstance',
                    uuid: this.instance?.uuid,
                    id: this.instance?.id,
                    version: versionID,
                },
                callback: (data: any) => {
                    if (data.status === 'success') {
                        this.sendMessage({
                            payload: {type: 'installedInstances'},
                            callback: (data: any) => {
                                this.storePacks(data);
                                this.finishInstall({
                                    modpackID,
                                    messageID: data.requestId,
                                });
                            },
                        });
                    } else if (data.status === 'error') {
                        this.updateInstall({
                            modpackID,
                            messageID: data.requestId,
                            error: true,
                            errorMessage: data.message,
                            instanceID: data.uuid,
                        });
                    } else if (data.overallPercentage < 100) {
                        this.updateInstall({
                            modpackID,
                            messageID: data.requestId,
                            progress: data.overallPercentage,
                            downloadSpeed: data.speed,
                            downloadedBytes: data.currentBytes,
                            totalBytes: data.overallBytes,
                            stage: data.currentStage,
                        });
                    } else if (data.currentStage === 'FINISHED') {
                        this.updateInstall({
                            modpackID,
                            messageID: data.requestId,
                            progress: data.overallPercentage,
                            downloadSpeed: data.speed,
                            downloadedBytes: data.currentBytes,
                            totalBytes: data.overallBytes,
                            stage: data.currentStage,
                        });
                    }
                    logVerbose(this.settingsState, 'Update data', JSON.stringify(data));
                },
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
        setTimeout(() => {
          this.hideAlert();
        }, 5000);
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
            this.instance.width = this.settingsState.hardware.supportedResolutions[
                data[0].id
                ].width;
            this.instance.height = this.settingsState.hardware.supportedResolutions[
                data[0].id
                ].height;

            this.saveSettings();
            return;
        }
    }

    public hideMsgBox(): void {
        this.showMsgBox = false;
    }

    private async mounted() {
        if (this.instance == null) {
            this.$router.push('/modpacks');
            return;
        }
        this.currentModpack = await this.fetchModpack(this.instance.id);
        this.$forceUpdate();
        if (this.currentModpack?.kind === 'modpack') {
          this.toggleChangelog(this.currentModpack?.versions[0].id);
        }
        this.getModList();
    }

    private getModList() {
      const modlistRaw = fs.readFileSync(this.instance?.path + '/version.json', 'utf-8');
      const modlistJson = JSON.parse(modlistRaw);
      modlistJson.files.forEach((mod: Modlist) => {
        if (mod.type === 'mod') {
          this.modlist.push({name: mod.name, version: mod.version, sha1: mod.sha1, size: mod.size});
        }
      });
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
            const changelog = await this.getChangelog({packID: this.instance?.id, versionID: id});
            this.changelogs[id] = changelog.content;
        }
      this.activeChangelog = id;
    }

    private debugLog(title: any, data: any) {
      logVerbose(this.settingsState, title, data);
    }
}
</script>
