<template>
    <div v-if="settingsState !== undefined" class="m-2 card" :class="`w-size-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}`">
        <div v-if="(!fake && (currentModpack !== undefined || instance !== undefined)) || isDemo" style="height: 100%">
            <article class="overflow-hidden shadow-lg relative" style="height: 100%">
                <img class="w-full pack-image rounded-sm"
                     :src="art !== undefined && art.length > 0 ? art : defaultImage" alt="placeholder"
                     :class="installing ? 'blur' : ''"/>
                <div class="content" :class="installing ? 'hide' : ''">
                    <!--        <div class="name-box">{{name}} (v{{version}})</div>-->
                    <div v-if="instance && !isLatestVersion" class="update-box">Update Available</div>
                    <div class="name-box"><p>{{name}}</p></div>
                </div>
                <div class="hoverContent" v-if="!installing">
                    <div :class="`row mb-2 min-h-size-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}`" >
                        <p :class="`font-bold text-text-color lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl text-center`">{{name}}</p>
                    </div>
                    <div class="row w-full" v-if="!isDemo">
                        <div class="buttons action-buttons w-full" v-if="installed">
                            <div @click="checkMemory()" class="cursor-pointer action-icon flex justify-center w-full items-center pl-2">
                                <font-awesome-icon  :icon="'play'" size="3x"
                                               :class="`cursor-pointer button lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`"/>
                                <p style="line-height: 1em;" :class="`ml-2 cursor-pointer lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`">Play</p>
                            </div>
                            <div class="action-icon  flex justify-center w-full cursor-pointer items-center pr-2" @click="goToInstance">
                                <font-awesome-icon :icon="'ellipsis-h'" size="3x"
                                               :class="`cursor-pointer button lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`"
                                               />
                                <p style="line-height: 1em;" :class="`ml-2 cursor-pointer lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`">More</p>
                            </div>
                        </div>
                        <div class="buttons action-buttons w-full" v-if="!installed">
                            <div @click="openInstall" class="cursor-pointer action-icon flex justify-center w-full items-center pl-2">
                            <font-awesome-icon :icon="'download'" size="3x"
                                               :class="`cursor-pointer button lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base `"/>
                                <p style="line-height: 1em;" :class="`ml-2 cursor-pointer lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`">Install</p>
                            </div>
                            <div class="action-icon  flex justify-center w-full cursor-pointer items-center pr-2"  @click="openInfo">
                            <font-awesome-icon :icon="'ellipsis-h'" size="3x"
                                               :class="`cursor-pointer button lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base `"
                                              />
                                <p style="line-height: 1em;" :class="`ml-2 cursor-pointer lg:text-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}xl sm:text-base`">More</p>
                            </div>
                        </div>
                    </div>
                    <div class="row mt-2">
                        <p class="font-bold text-text-color sm:text-sm lg:text-lg">v{{version}}</p>
                    </div>
                </div>
                <div class="hoverContent show" v-else>
                    <div class="row mb-2">
                        <p class="font-bold text-text-color lg:text-2xl text-center">Installing {{name}}</p>
                    </div>
                    <div class="row">
                        <font-awesome-icon :icon="'spinner'" size="5x"
                                           class="cursor-pointer button hover-scale lg:text-5xl sm:text-base" spin/>
                    </div>
                </div>
            </article>
        </div>
        <FTBModal :visible="showInstall" @dismiss-modal="hideInstall">
            <InstallModal :pack-name="name" :doInstall="install" :pack-description="description" :versions="versions"/>
        </FTBModal>
        <FTBModal :visible="showMsgBox" @dismiss-modal="hideMsgBox">
            <message-modal :title="msgBox.title" :content="msgBox.content" :ok-action="msgBox.okAction"
                           :cancel-action="msgBox.cancelAction" :type="msgBox.type"/>
        </FTBModal>
    </div>
    <!-- <div class="text-gray-700 text-center flex-1 m-2 sm:min-w-psm sm:max-w-psm sm:min-h-psm sm:max-h-psm md:min-w-pmd md:max-w-pmd md:min-h-pmd md:max-h-pmd lg:min-w-plg lg:max-w-plg lg:min-h-plg lg:max-h-plg card">
      <div class="bg-image" v-bind:style="{'background-image': `url(${art})`}" :class="installing ? 'blur' : ''">
      </div>
      <div class="content" :class="installing ? 'hide' : ''">
        <div class="name-box">{{name}} (v{{version}})</div>
      </div>
      <div class="hoverContent" v-if="!installing">
        <div class="row mb-2">
          <p class="font-bold text-text-color lg:text-2xl">{{name}}</p>
        </div>
        <div class="row">
          <div class="buttons" v-if="installed">
            <font-awesome-icon @click="launch()" :icon="'play'" size="3x"
                               class="cursor-pointer button hover-scale lg:text-5xl sm:text-base"/>
            <div class="divider"></div>
            <font-awesome-icon :icon="'ellipsis-h'" size="3x" class="cursor-pointer button hover-scale lg:text-5xl sm:text-base"
                               @click="goToInstance"/>
          </div>
          <div class="buttons" v-if="!installed">
            <font-awesome-icon @click="openInstall" :icon="'download'" size="3x"
                               class="cursor-pointer button hover-scale lg:text-5xl sm:text-base"/>
            <div class="divider"></div>
            <font-awesome-icon :icon="'info-circle'" size="3x"
                               class="cursor-pointer button hover-scale lg:text-5xl sm:text-base" @click="openInfo"/>
          </div>
        </div>
        <div class="row mt-2">
          <p class="font-bold text-text-color sm:text-sm lg:text-lg">v{{version}}</p>
        </div>
      </div>
      <div class="hoverContent show" v-else>
        <div class="row mb-2">
          <p class="font-bold text-text-color lg:text-2xl">Installing {{name}}</p>
        </div>
        <div class="row">
          <font-awesome-icon :icon="'spinner'" size="5x"
                               class="cursor-pointer button hover-scale lg:text-5xl sm:text-base" spin/>
        </div>
      </div>
      <FTBModal :visible="showInstall" @dismiss-modal="hideInstall">
        <InstallModal :pack-name="name" :doInstall="install" :pack-description="description" :versions="versions"/>
      </FTBModal>
    </div> -->
</template>

<script lang="ts">
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';
import FTBModal from '@/components/FTBModal.vue';
import SettingsModal from '@/components/modals/SettingsModal.vue';
import InformationModal from '@/components/modals/InformationModal.vue';
import InstallModal from '@/components/modals/InstallModal.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import {Action, State} from 'vuex-class';
import {ModpackState, Versions, Instance} from '../../modules/modpacks/types';
    // @ts-ignore
import placeholderImage from '@/assets/placeholder_art.png';
import semver from 'semver';
import { SettingsState } from '@/modules/settings/types';

const namespace: string = 'websocket';

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
        ],
    })
    export default class PackCard extends Vue {
        @State('modpacks') public modpacks!: ModpackState;
        @Action('sendMessage') public sendMessage: any;
        @Action('updateInstall', {namespace: 'modpacks'}) public updateInstall: any;
        @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack: any;
        @Action('finishInstall', {namespace: 'modpacks'}) public finishInstall: any;
        @Action('errorInstall', {namespace: 'modpacks'}) public errorInstall: any;
        @Action('storeInstalledPacks', {namespace: 'modpacks'}) public storePacks: any;
        @State('settings') public settingsState!: SettingsState;

        public name!: string;
        @Prop()
        public instance!: Instance;
        @Prop()
        public packID!: number;
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

        @Watch('modpacks', {deep: true})
        public onModpacksChange(newState: ModpackState, oldState: ModpackState) {
            console.log('State updated');
            this.$forceUpdate();
        }

        public async mounted() {
            if (this.instance !== undefined) {
                this.fetchModpack(this.instance.id);
            }
        }

        public checkMemory() {
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

        // @ts-ignore
        public launch(): void {
            this.sendMessage({
                payload: {type: 'launchInstance', uuid: this.$props.instanceID}, callback: (data: any) => {
                    // Instance launched
                },
            });
        }

        public hideMsgBox(): void {
            this.showMsgBox = false;
        }

        get installing() {
            return this.modpacks !== undefined && this.modpacks.installing.indexOf(this.modpacks.installing.filter((pack) => pack.modpackID === this.$props.packID)[0]) !== -1;
        }

        public install(version: number): void {
            this.updateInstall({modpackID: this.$props.packID, progress: 0});
            this.sendMessage({
                payload: {type: 'installInstance', id: this.$props.packID, version}, callback: (data: any) => {
                    if (this.showInstall) {
                        this.showInstall = false;
                    }
                    if (data.status === 'success') {
                        this.sendMessage({
                            payload: {type: 'installedInstances'}, callback: (data: any) => {
                                this.storePacks(data);
                                this.finishInstall({modpackID: this.$props.packID, messageID: data.requestId});
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

        public deleteInstace(): void {
            this.sendMessage({
                payload: {type: 'uninstallInstance', uuid: this.$props.instanceID}, callback: (data: any) => {
                    this.sendMessage({
                        payload: {type: 'installedInstances'}, callback: (data: any) => {
                            this.storePacks(data);
                        },
                    });
                },
            });
        }

        public goToInstance(): void {
            this.$router.push({name: 'instancepage', query: {uuid: this.$props.instanceID}});
        }

        public openInfo(): void {
            this.$router.push({name: 'modpackpage', query: {modpackid: this.$props.packID}});
        }

        public openInstall(): void {
            this.showInstall = true;
        }

        public hideInstall(): void {
            this.showInstall = false;
        }

        get latestVersion() {
            return this.modpacks?.packsCache[this.instance.id].versions.sort((a, b) => {
                return semver.rcompare(a.name, b.name);
            });
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
    }
</script>

<style scoped lang="scss">
    .card {
        position: relative;
    }

    .pack-image {
        transition: filter .5s;
        height: 100%;
        object-fit: cover;
    }

    .card:hover .pack-image {
        filter: blur(5px) brightness(50%);
    }

    .pack-image.blur {
        filter: blur(5px) brightness(50%);
    }

    .card:hover .hoverContent {
        opacity: 1;
    }

    .hoverContent.show {
        opacity: 1;
    }

    .card:hover .content {
        opacity: 0;
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
        width: 100%;
        color: #fff;
        bottom: 0;
        opacity: 1;
        transition: opacity .3s;
        z-index: 2;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;
        align-items: flex-end;
    }

    .content .name-box {
        backdrop-filter: blur(8px);
        background: linear-gradient(to top, rgba(36, 40, 47, 0) 0%, rgba(43, 57, 66, 0.2) calc( 100% - 2px), rgba(193, 202, 207, 0.1) calc( 100% - 1px), rgba(29, 29, 29, 0.3) 100%);
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
        text-align: center;
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
        transition: opacity .5s;
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
        transition: transform .2s ease-in;
    }

    // .hover-scale:hover {
    //     transform: scale(1.3);
    // }

    .button-shadow {
        // text-shadow: 3px 6px #272634;
        filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, .8));
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
        transition: width .2s, filter .4s;
        overflow:hidden;
        & > * {
            overflow:hidden;
        }
        & p.ml-2 {
            display: none;
        }
    }
    .action-buttons .action-icon:hover:not(.divider), .action-buttons .action-icon:hover:not(.divider) ~ .action-icon:hover:not(.divider) {
        width: 100%;
        & p.ml-2  {
            display: block;
        }
    }
</style>
