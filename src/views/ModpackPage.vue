<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div class="flex flex-col h-full" v-if="currentModpack != undefined">
      <div>
        <div
            class="header-image"
            v-bind:style="{'background-image': `url(${currentModpack.art.filter((art) => art.type === 'splash').length > 0 ? currentModpack.art.filter((art) => art.type === 'splash')[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'})`}"
        >
          <span class="instance-name text-4xl">{{currentModpack.name}}</span>
          <span class="instance-info">
            <small>
              <em>{{currentModpack.synopsis}}</em>
            </small>
            <div v-if="currentModpack.tags" class="flex flex-row items-center">
                <div class="flex flex-row">
                    <span v-for="(tag, i) in limitedTags" :key="`tag-${i}`" @click="clickTag(tag.name)" class="rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
                    <span v-if="currentModpack.tags.length > 5" :key="`tag-more`" class="rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+{{currentModpack.tags.length - 5}}</span>
                </div>
            </div>
          </span>
          <div class="update-bar"  v-if="currentModpack && currentModpack.notification">
            {{currentModpack.notification}}
          </div>
          <div class="instance-buttons flex flex-row frosted-glass">
            <div class="instance-button mr-1">
              <ftb-button class="py-2 px-4" color="primary" css-class="text-center text-l" @click="install(currentModpack.versions[0].id)"><font-awesome-icon icon="download" size="1x"/> Install</ftb-button>
<!--              <button-->
<!--                  class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center"-->
<!--                  @click="install(currentModpack.versions[0].id)"-->
<!--              >-->
<!--                <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/>&nbsp;Install</span>-->
<!--              </button>-->
            </div>
          </div>
        </div>
      </div>
      <div style="height: auto; flex:1; overflow-y: auto;" class="flex flex-col">
        <ul class="flex p-2 pb-0" style="position: sticky; top: 0; background: #2A2A2A; padding-bottom: 0;">
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
        </ul>

        <div class="tab-content bg-navbar flex-1 p-2 py-4 mx-2" style="overflow-y: auto;">
          <div class="tab-pane" v-if="isTabActive('overview')" id="overview">
            <div class="flex flex-wrap" v-if="currentModpack != null">
              <VueShowdown :markdown="currentModpack.description" :extensions="['classMap', 'attribMap', 'newLine']"/>
            </div>
            <hr/>

            <!-- <div class="versions" v-if="currentModpack != null && currentModpack.versions != null">
          <div v-for="(version, index) in currentModpack.versions" v-bind:key="index">
            {{version.name}}

            <span class="inline-flex bg-blue-600 text-white rounded-full h-6 px-3 justify-center items-center" v-if="version.type == 'Beta'">{{version.type}}</span>
            <span class="inline-flex bg-green-600 text-white rounded-full h-6 px-3 justify-center items-center" v-if="version.type == 'Release'">{{version.type}}</span>

            </div>-->
            <!-- </div> -->
          </div>
          <div class="tab-pane" v-if="isTabActive('versions')" id="versions">
            <div v-for="(version, index) in currentModpack.versions" :key="index">
              <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                <p>{{currentModpack.name}} - {{version.name}}</p>
                <span @click="toggleChangelog(version.id)" class="pl-5 cursor-pointer"><font-awesome-icon
                    :icon="activeChangelog === version.id ? 'chevron-down' : 'chevron-right'" class="cursor-pointer"
                    size="1x"/> Changelog</span>
                <ftb-button class="py-2 px-4 ml-auto" color="primary" css-class="text-center text-l" @click="install(version.id)"><font-awesome-icon icon="download" size="1x"/> Install</ftb-button>
<!--                <button-->
<!--                    class="bg-blue-500 hover:bg-blue-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-pointer"-->
<!--                    @click="install(version.id)"-->
<!--                >-->
<!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/>&nbsp;Install</span>-->
<!--                </button>-->
                <v-selectmenu :title="false" :query="false" :data="serverDownloadMenu(version.id)" align="right" type="regular">
                  <ftb-button class="py-2 px-4 ml-5" color="info" css-class="text-center text-l"><font-awesome-icon icon="download" size="1x"/> Download Server</ftb-button>
<!--                  <button type="button" class="bg-orange-500 hover:bg-orange-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer"><span><font-awesome-icon icon="download" size="1x"/> Download Server</span></button>-->
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
                <VueShowdown :markdown="changelogs[version.id]" :extensions="['classMap', 'newLine']"/>
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
    background: rgba(	255, 193, 7, 0.9);
    width: 100%;
    height: 25px;
    text-align: left;
    font-weight: 700;
    padding: 2px 2px 2px 6px;
    color: black;
  }
    .frosted-glass {
      backdrop-filter: blur(8px);
      background: linear-gradient(to top, rgba(36, 40, 47, 0) 0%, rgba(43, 57, 66, 0.2) calc( 100% - 2px), rgba(193, 202, 207, 0.1) calc( 100% - 1px), rgba(29, 29, 29, 0.3) 100%);
    //   -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,1) 50%, rgba(0,0,0,0) 100%);
  }
</style>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {ModpackState, ModPack, Instance} from '@/modules/modpacks/types';
import {State, Action} from 'vuex-class';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import config from '@/config';
import moment from 'moment';
import MessageModal from '@/components/modals/MessageModal.vue';
import FTBModal from '@/components/FTBModal.vue';

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
    name: 'ModpackPage',
    components: {
        'ftb-input': FTBInput,
        'ftb-toggle': FTBToggle,
        'ftb-slider': FTBSlider,
        'ftb-button': FTBButton,
        FTBModal,
        'message-modal': MessageModal,
    },
})
export default class ModpackPage extends Vue {

    get limitedTags() {
        if (this.currentModpack && this.currentModpack.tags) {
            return this.currentModpack.tags.slice(0, 5);
        } else {
            return [];
        }
    }
    get currentModpack() {
      const id: number = Number(this.$route.query.modpackid);
      return this.modpacks?.packsCache[id];
    }
    @State('modpacks') public modpacks: ModpackState | undefined = undefined;
    @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack!: any;
    @Action('storeInstalledPacks', {namespace: 'modpacks'}) public storePacks!: any;
    @Action('updateInstall', {namespace: 'modpacks'}) public updateInstall!: any;
    @Action('finishInstall', {namespace: 'modpacks'}) public finishInstall!: any;
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

    public serverDownloadMenu(versionID: number) {
        const links = [];
        links.push({content: 'Windows', url: `${config.apiURL}/public/modpack/${this.currentModpack?.id}/${versionID}/server/windows`, open: '_blank'});
        links.push({content: 'Linux', url: `${config.apiURL}/public/modpack/${this.currentModpack?.id}/${versionID}/server/linux`, open: '_blank'});
        links.push({content: 'MacOS', url: `${config.apiURL}/public/modpack/${this.currentModpack?.id}/${versionID}/server/mac`, open: '_blank'});
        return links;
    }

    public hideMsgBox(): void {
        this.showMsgBox = false;
    }

    public isTabActive(tabItem: string) {
        return this.activeTab === tabItem;
    }

    public setActiveTab(tabItem: string) {
        this.activeTab = tabItem;
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

    public install(version: number): void {
        this.updateInstall({modpackID: this.$route.query.modpackid, progress: 0});
        this.sendMessage({
            payload: {type: 'installInstance', id: this.$route.query.modpackid, version}, callback: (data: any) => {
                if (data.status === 'success') {
                    this.sendMessage({
                        payload: {type: 'installedInstances'}, callback: async (installList: any) => {
                            await this.storePacks(installList);
                            await this.finishInstall({
                                modpackID: this.$route.query.modpackid,
                                messageID: installList.requestId,
                            });
                            this.$router.push({name: 'instancepage', query: {uuid: data.uuid}});
                        },
                    });
                } else if (data.status === 'error') {
                    this.updateInstall({
                        modpackID: this.$route.query.modpackid,
                        messageID: data.requestId,
                        error: true,
                        errorMessage: data.message,
                        instanceID: data.uuid,
                    });
                } else if (data.currentStage === 'POSTINSTALL') {
                    // We don't care about this, keep progress bar showing.
                } else if (data.status === 'init') {
                    this.updateInstall({
                        modpackID: this.$route.query.modpackid,
                        messageID: data.requestId,
                        stage: 'INIT',
                        message: data.message,
                    });
                } else if (data.overallPercentage <= 100) {
                    this.updateInstall({
                        modpackID: this.$route.query.modpackid,
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

    public clickTag(tagName: string) {
        this.$router.push({name: 'browseModpacks', params: {search: tagName}});
    }

    private async mounted() {
        await this.fetchModpack(this.$route.query.modpackid);
        this.toggleChangelog(this.currentModpack?.versions[0].id);
    }

    private async toggleChangelog(id: number | undefined) {
        if (typeof id === 'undefined') {
            return;
        }
        if (!this.changelogs[id]) {
            const changelog = await this.getChangelog({packID: this.currentModpack?.id, versionID: id});
            this.changelogs[id] = changelog.content;
        }
        this.activeChangelog = id;
    }

}
</script>
