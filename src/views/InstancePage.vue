<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div class="flex flex-col h-full" v-if="instance != undefined">
      <div>
        <div
            class="header-image"
            v-bind:style="{'background-image': `url(${currentModpack.art.filter((art) => art.type === 'splash').length > 0 ? currentModpack.art.filter((art) => art.type === 'splash')[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'})`}"
        >
          <span class="instance-name text-4xl">{{instance.name}}</span>
          <span class="instance-info">
            <small>
              {{currentModpack.name}} <span v-for="author in currentModpack.authors">By {{author.name}}</span> - {{instance.version}} -
              <em>{{currentModpack.synopsis}}</em>
            </small>
          </span>
          <div class="update-bar"  v-if="instance && !isLatestVersion">
            A new update is available
          </div>
          <div class="instance-buttons flex flex-row">
            <div class="instance-button mr-1">
              <button
                  class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"
                  @click="checkMemory()"
              >
                <span class="cursor-pointer"><font-awesome-icon icon="play" size="1x"/> Play</span>
              </button>
            </div>
            <div class="instance-button mr-1"
                  v-if="instance && !isLatestVersion">
              <button
                  class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"
                  @click="update()"
              >
                <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> Update</span>
              </button>
            </div>
            <div class="instance-button mr-1">
              <div class="text-white-500 py-2 px-4 inline-flex items-center">
                <font-awesome-icon icon="stopwatch" size="1x"/>&nbsp;
                <small
                    class="text-gray-400 italic"
                >{{instance.lastPlayed | moment("from", "now")}}</small>
              </div>
            </div>
            <div class="instance-button mr-2 ml-auto">
              <button
                  class="bg-red-700 hover:bg-red-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"
                  @click="confirmDelete()"
              >
                <span class="cursor-pointer"><font-awesome-icon icon="trash" size="1x"/> Delete</span>
              </button>
            </div>
            <div class="instance-button mr-2">
              <button
                  class="bg-orange-700 hover:bg-orange-600 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer"
                  @click="browseInstance()"
              >
                <span class="cursor-pointer"><font-awesome-icon icon="folder" size="1x"/> Open Folder</span>
              </button>
            </div>
          </div>
        </div>
      </div>
      <div style="height: auto; flex:1; overflow-y: auto;">
        <ul class="flex border-b border-gray-600 mb-3 p-2" style="position: sticky; top: 0; background: #2A2A2A; padding-bottom: 0;">
          <li class="-mb-px mr-1">
            <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('overview')"
                :class="{ 'border-l border-t border-r border-gray-600': isTabActive('overview'), 'text-gray-600 hover:text-gray-500': !isTabActive('overview') }"
                href="#overview"
            >Overview</a>
          </li>
          <li class="-mb-px mr-1">
            <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('versions')"
                :class="{ 'border-l border-t border-r border-gray-600': isTabActive('versions'), 'text-gray-600 hover:text-gray-500': !isTabActive('versions') }"
                href="#versions"
            >Versions</a>
          </li>
          <li class="-mb-px mr-1">
            <a
                    class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                    @click.prevent="setActiveTab('modlist')"
                    :class="{ 'border-l border-t border-r border-gray-600': isTabActive('modlist'), 'text-gray-600 hover:text-gray-500': !isTabActive('modlist') }"
                    href="#versions"
            >ModList</a>
          </li>
          <li class="-mb-px mr-1">
            <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('settings')"
                :class="{ 'border-l border-t border-r border-gray-600': isTabActive('settings'), 'text-gray-600 hover:text-gray-500': !isTabActive('settings') }"
                href="#settings"
            >Settings</a>
          </li>
        </ul>
        <div class="tab-content p-2" style="overflow-y: auto; flex: 1; margin-bottom: 40px;">
          <div class="tab-pane" v-if="isTabActive('overview')" id="overview">
            <div class="flex flex-wrap" v-if="currentModpack != null">
              <hr/>
              <VueShowdown :markdown="currentModpack.description" :extensions="['classMap', 'attribMap', 'newLine']"/>
            </div>
            <hr/>
          </div>
          <div class="tab-pane" v-if="isTabActive('versions')" id="versions">
            <div v-for="(version, index) in currentModpack.versions" :key="index">
              <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                <p>{{currentModpack.name}} - {{version.name}}</p>
                <span @click="toggleChangelog(version.id)" class="pl-5 cursor-pointer"><font-awesome-icon
                    :icon="activeChangelog === version.id ? 'chevron-down' : 'chevron-right'" class="cursor-pointer"
                    size="1x"/> Changelog</span>
                <button
                    v-if="instance.versionId && instance.versionId !== version.id"
                    class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-pointer"
                    @click="update(version.id)"
                >
                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> {{isOlderVersion(version.name) ? 'Downgrade' : 'Update'}}</span>
                </button>
                <button
                    v-if="instance.versionId && instance.versionId === version.id"
                    class="bg-blue-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-not-allowed"
                >
                  <span class="cursor-not-allowed"><font-awesome-icon icon="check" size="1x"/> Current</span>
                </button>
                <v-selectmenu :title="false" :query="false" :data="serverDownloadMenu(version.id)" align="right" type="regular">
                  <button type="button" class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer"><span><font-awesome-icon icon="download" size="1x"/> Download Server</span></button>
                </v-selectmenu>
<!--                <button @click="downloadServer"-->
<!--                        class="bg-orange-500 hover:bg-blue-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer">-->
<!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> Download Server</span>-->
<!--                </button>-->
              </div>
              <div class="pl-5" v-if="activeChangelog === version.id">
<!--                <code class="p-0">-->
<!--                  {{changelogs[version.id]}}-->
<!--                </code>-->
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
                    <button
                        class="appearance-none block w-full bg-green-400 text-white-600 border border-green-400 py-3 px-4 leading-tight"
                    >
                      <font-awesome-icon icon="desktop" size="1x"/>
                    </button>
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
                  unit="MB"
              />
              <ftb-input label="Custom Arguments" v-model="instance.jvmArgs" @blur="saveSettings"/>
              <button
                  class="cursor-pointer bg-green-500 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto mr-2 mb-2"
              >
                <font-awesome-icon icon="save" size="1x"/>&nbsp;Save
              </button>
            </div>
          </div>

          <div class="tab-pane" v-if="isTabActive('modlist')" id="modlist">
            <div v-for="file in modlist">
              <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                <p>{{file.name.replace('.jar', '')}} - {{file.version}} (Size: {{parseInt(file.size) | prettyBytes}})</p>
                <p class="ml-auto">SHA1: {{file.sha1}}</p>
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
    <iframe></iframe>
  </div>
</template>

<style lang="scss">
  .header-image {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 300px;
    transition: all 0.2s ease-in-out;
  }

  @media screen and (max-height: 800px) {
    .header-image {
      height: 150px;
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
import fs from 'fs';

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

    get currentModpack(){
      if(!this.instance){
        return null;
      }
      return this.modpacks?.packsCache[this.instance.id];
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

    public serverDownloadMenu(versionID: number) {
        const links = [];
        links.push({content: 'Windows', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/windows`, open: '_blank'});
        links.push({content: 'Linux', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/linux`, open: '_blank'});
        links.push({content: 'MacOS', url: `${config.apiURL}/public/modpack/${this.instance?.id}/${versionID}/server/mac`, open: '_blank'});
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
            if (versionID === undefined) {
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
                    console.log(JSON.stringify(data));
                },
            });
        }
    }

    public async saveSettings() {
        await this.saveInstance(this.instance);
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
        await this.fetchModpack(this.instance.id);
        this.toggleChangelog(this.modpacks?.currentModpack?.versions[0].id);
        this.getModList();
    }

    private getModList(){
      const modlistRaw = fs.readFileSync(this.instance?.path + '/version.json', 'utf-8')
      const modlistJson = JSON.parse(modlistRaw);
      modlistJson.files.forEach((mod: Modlist) => {
        if (mod.type === 'mod') {
          this.modlist.push({name: mod.name, version: mod.version, sha1: mod.sha1, size: mod.size})
        }
      });
    }

    private async toggleChangelog(id: number | undefined) {
        if (typeof id === 'undefined') {
            return;
        }
        if (!this.changelogs[id]) {
            const changelog = await this.getChangelog({packID: this.modpacks?.currentModpack?.id, versionID: id});
            this.changelogs[id] = changelog.content;
        }
        this.activeChangelog = id;
    }

    get isLatestVersion(){
      if(this.currentModpack === undefined){
        return true;
      }
      return this.instance?.versionId === this.currentModpack?.versions[0].id;
    }
}
</script>
