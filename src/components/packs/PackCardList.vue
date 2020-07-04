<template>
    <div v-if="settingsState !== undefined" class="m-2 card-list" :class="`w-full h-size-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}`">
        <div v-if="(!fake && (currentModpack !== undefined || instance !== undefined)) || isDemo" style="height: 100%" class="flex flex-row">
            <article :class="`relative overflow-hidden shadow-lg w-size-${size ? size : settingsState.settings.packCardSize ? settingsState.settings.packCardSize : 2}`" style="height: 100%">
                <img class="pack-image rounded-sm"
                     :src="art !== undefined && art.length > 0 ? art : defaultImage" alt="placeholder"
                     />
                <div class="content" >
                    <!--        <div class="name-box">{{name}} (v{{version}})</div>-->
                    <div v-if="instance && !isLatestVersion" class="update-box">New Version</div>
                    <div v-if="installing" class="update-box">Installing...</div>
                </div>
            </article>
            <div class="flex-1 p-2 bg-background-lighten flex flex-col">
                <div class="name-box font-bold">{{name}}</div>
                <p class="mb-auto">{{description}}</p>
                <div  v-if="tags" class="flex flex-row items-center">
                    <div class="flex flex-row">
                        <span v-for="(tag, i) in limitedTags" :key="`tag-${i}`" @click="clickTag(tag.name)" class="cursor-pointer rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
                        <span v-if="tags.length > 5" :key="`tag-more`" class=" rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+{{tags.length - 5}}</span>
                    </div>
                </div>
            </div>
            <div style="width:50px;" class="flex flex-col list-action-button-holder" v-if="installed">  
                <FTBButton @click="checkMemory()" :isRounded="false" color="primary" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-tr"><font-awesome-icon icon="play" size="sm" class="cursor-pointer"/><p>Play</p></FTBButton>
                <FTBButton @click="goToInstance" :isRounded="false" color="info" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-br"><font-awesome-icon icon="ellipsis-h" size="sm" class="cursor-pointer"/><p>More</p></FTBButton>
            </div>
            <div style="width:50px;" class="flex flex-col list-action-button-holder" v-if="!installed">
                <FTBButton @click="openInstall" :isRounded="false" color="primary" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-tr"><font-awesome-icon icon="download" size="sm" class="cursor-pointer"/><p>Get</p></FTBButton>
                <FTBButton @click="openInfo" :isRounded="false" color="info" class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-br"><font-awesome-icon icon="info-circle" size="sm" class="cursor-pointer"/><p>Info</p></FTBButton>
            </div>
        </div>
        <FTBModal :visible="showInstall" @dismiss-modal="hideInstall">
            <InstallModal :pack-name="name" :doInstall="install" :pack-description="description" :versions="versions"/>
        </FTBModal>
        <FTBModal :visible="showMsgBox" @dismiss-modal="hideMsgBox">
            <message-modal :title="msgBox.title" :content="msgBox.content" :ok-action="msgBox.okAction"
                           :cancel-action="msgBox.cancelAction" :type="msgBox.type"/>
        </FTBModal>
    </div>
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
            'tags',
        ],
    })
    export default class PackCardList extends Vue {
        @State('modpacks') public modpacks!: ModpackState;
        @Action('sendMessage') public sendMessage: any;
        @Action('updateInstall', {namespace: 'modpacks'}) public updateInstall: any;
        @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack: any;
        @Action('finishInstall', {namespace: 'modpacks'}) public finishInstall: any;
        @Action('errorInstall', {namespace: 'modpacks'}) public errorInstall: any;
        @Action('storeInstalledPacks', {namespace: 'modpacks'}) public storePacks: any;
        @State('settings') public settingsState!: SettingsState;
        @Action('doSearch', {namespace: 'modpacks'}) public doSearch: any;

        public name!: string;
        @Prop()
        public instance!: Instance;
        @Prop()
        public packID!: number;
        @Prop()
        public tags!: [];
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

        public clickTag(tagName: string){
            this.$router.push({name: 'browseModpacks', params: {search: tagName}})
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

        get limitedTags(){
            if(this.tags){
                return this.tags.slice(0, 5);
            } else {
                return [];
            }
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
    .card-list .list-action-button {
        filter: brightness(0.7);
    }
    .card-list:hover .list-action-button {
        filter: brightness(1);
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
        transition: height .2s, filter .4s;
        overflow:hidden;
        & > * {
            overflow:hidden;
        }
        & p:not(.cursor-pointer) {
            display: none;
        }
    }
    .list-action-button-holder .list-action-button:hover, .list-action-button-holder .list-action-button:hover ~ .list-action-button:hover {
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
        bottom: 0;
        width: 100%;
        color: #fff;
        opacity: 1;
        transition: opacity .3s;
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

    .hover-scale:hover {
        transform: scale(1.3);
    }

    .button-shadow {
        // text-shadow: 3px 6px #272634;
        filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, .8));
    }
</style>
