<template>
  <div class="w-1/3 md:w-1/3 lg:w-1/11 xl:w-1/11 m-2 card">
  <div v-if="!fake" style="height: 100%">
    <article class="overflow-hidden shadow-lg" style="height: 100%">
      <img class="w-full pack-image rounded-sm" :src="art.length > 0 ? art : '../../assets/placeholder_art.png'" alt="placeholder" :class="installing ? 'blur' : ''"/>
      <div class="content" :class="installing ? 'hide' : ''">
<!--        <div class="name-box">{{name}} (v{{version}})</div>-->
        <div class="name-box">{{name}}</div>
      </div>
      <div class="hoverContent" v-if="!installing">
        <div class="row mb-2">
          <p class="font-bold text-text-color lg:text-2xl text-center">{{name}}</p>
        </div>
        <div class="row">
          <div class="buttons" v-if="installed">
            <font-awesome-icon @click="checkMemory()" :icon="'play'" size="3x"
                              class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"/>
            <div class="divider"></div>
            <font-awesome-icon :icon="'ellipsis-h'" size="3x" class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"
                              @click="goToInstance"/>
          </div>
          <div class="buttons" v-if="!installed">
            <font-awesome-icon @click="openInstall" :icon="'download'" size="3x"
                              class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"/>
            <div class="divider"></div>
            <font-awesome-icon :icon="'info-circle'" size="3x"
                              class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow" @click="openInfo"/>
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
import { ModpackState, Versions, Instance } from '../../modules/modpacks/types';
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
    ],
})
export default class PackCard extends Vue {
    @State('modpacks') public modpacks: ModpackState | undefined = undefined;
    @Action('sendMessage') public sendMessage: any;
    @Action('updateInstall', {namespace: 'modpacks'}) public updateInstall: any;
    @Action('finishInstall', {namespace: 'modpacks'}) public finishInstall: any;
    @Action('errorInstall', {namespace: 'modpacks'}) public errorInstall: any;
    @Action('storeInstalledPacks', {namespace: 'modpacks'}) public storePacks: any;

    public name!: string;
    @Prop()
    public instance!: Instance;
    private showInstall: boolean = false;
    private showMsgBox: boolean = false;
    private msgBox: MsgBox = {
        title: '',
        content: '',
        type: '',
        okAction: Function,
        cancelAction: Function,
    };

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
      this.sendMessage({payload: {type: 'launchInstance', uuid: this.$props.instanceID}, callback: (data: any) => {
        // Instance launched
      }});
    }

    public hideMsgBox(): void {
        this.showMsgBox = false;
    }

    get installing() {
      return this.modpacks !== undefined && this.modpacks.installing.indexOf(this.modpacks.installing.filter((pack) => pack.modpackID === this.$props.packID)[0]) !== -1;
    }

    public install(version: number): void {
      this.updateInstall({modpackID: this.$props.packID, progress: 0});
      this.sendMessage({payload: {type: 'installInstance', id: this.$props.packID, version}, callback: (data: any) => {
        if (this.showInstall) {
          this.showInstall = false;
        }
        if (data.status === 'success') {
          this.sendMessage({payload: {type: 'installedInstances'}, callback: (data: any) => {
            this.storePacks(data);
            this.finishInstall({modpackID: this.$props.packID, messageID: data.requestId});
          }});
        } else if (data.status === 'error') {
          this.updateInstall({modpackID: this.$props.packID, messageID: data.requestId, error: true, errorMessage: data.message, instanceID: data.uuid});
        } else if (data.currentStage === 'POSTINSTALL') {
          this.updateInstall({modpackID: this.$props.packID, messageID: data.requestId, stage: data.currentStage});
        } else if (data.status === 'init') {
          this.updateInstall({modpackID: this.$props.packID, messageID: data.requestId, stage: 'INIT', message: data.message});
        } else if (data.overallPercentage <= 100) {
          this.updateInstall({modpackID: this.$props.packID, messageID: data.requestId, progress: data.overallPercentage, downloadSpeed: data.speed, downloadedBytes: data.currentBytes, totalBytes: data.overallBytes, stage: data.currentStage});
        }
      }});
    }

    public deleteInstace(): void {
      this.sendMessage({payload: {type: 'uninstallInstance', uuid: this.$props.instanceID}, callback: (data: any) => {
        this.sendMessage({payload: {type: 'installedInstances'}, callback: (data: any) => {
            this.storePacks(data);
          }});
      }});
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
}
</script>

<style lang="scss">
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
  .content.hide {
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

  .button-shadow{
    // text-shadow: 3px 6px #272634;
    filter: drop-shadow( 10px 10px 5px rgba(0, 0, 0, .8));
  }
</style>
