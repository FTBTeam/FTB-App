<template>
   <modal :open="open" @closed="close" :close-on-background-click="!installing"
          :title="mod ? mod.name : 'Loading...'" sub-title="Select the version you want to install" :external-contents="true"
   >
     <modal-body v-if="mod">
       <div class="py-6" v-if="!installing && !finishedInstalling">
         <div class="flex gap-4 mb-4">
           <font-awesome-icon icon="download" size="xl" />
           <b class="text-lg block">Install {{ mod.name }}</b>
         </div>
         
         <selection2
           label="Selection mod version"
           v-model="selectedVersion"
           :options="options"
         />
       </div>

       <div v-if="!installing && !finishedInstalling" class="pt-8">
         <div class="flex items-center gap-4 mb-6">
           <img src="@/assets/curse-logo.svg" alt="CurseForge logo" width="40" />
           <b class="text-lg block">About mod installs</b>
         </div>
         <hr class="curse-border block mb-4" />
         <p class="text-muted mb-4">🎉 Each mod install from the FTB App directly supports the mod developers through the CurseForge reward system!</p>
         <b class="block mb-2">Dependencies</b>
         <p class="mb-2 text-muted">If the mod depends on other mods on the CurseForge platform, the app will try and resolve these dependencies for you and install them as well.</p>
         <p class="text-muted">Sometimes this does not work and you'll have to install the dependencies manually.</p>
       </div>

       <div class="installing mt-6 mb-4" v-if="!finishedInstalling && installing">
         <div class="progress font-bold"><font-awesome-icon icon="spinner" spin class="mr-2" /> Installing</div>
<!--         <div class="stats">-->
<!--           <div class="stat">-->
<!--             <div class="text">Progress</div>-->
<!--             <div class="value">{{ installProgress.percentage }}%</div>-->
<!--           </div>-->
<!--           <div class="stat">-->
<!--             <div class="text">Speed</div>-->
<!--             <div class="value">{{ prettyBytes(installProgress.speed) }}/s</div>-->
<!--           </div>-->

<!--           <div class="stat">-->
<!--             <div class="text">Downloaded</div>-->
<!--             <div class="value">-->
<!--               {{ prettyBytes(installProgress.current) }} / {{ prettyBytes(installProgress.total) }}-->
<!--             </div>-->
<!--           </div>-->
<!--         </div>-->
       </div>

       <p v-if="!installing && finishedInstalling">
         <span class="block">{{ mod.name }} has been installed!</span>
       </p>
     </modal-body>

     <modal-footer>
       <div class="flex justify-end gap-4" v-if="!installing || !finishedInstalling">
         <ui-button type="success" icon="download" v-if="!installing && !finishedInstalling" :disabled="!selectedVersion" @click="installMod">Install</ui-button>
         <ui-button type="primary" @click="close" icon="check" v-if="finishedInstalling">Close</ui-button>
       </div>
     </modal-footer>
   </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import UiButton from '@/components/core/ui/UiButton.vue';
import Selection2 from '@/components/core/ui/Selection2.vue';
import {emitter, getColorForReleaseType, prettyByteFormat} from '@/utils';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Mod} from '@/types';
import {InstanceJson} from '@/core/@types/javaApi';
import {compatibleCrossLoaderPlatforms} from '@/utils/helpers/packHelpers';

// TODO: Remove
type InstallProgress = {
  percentage: number;
  speed: number;
  current: number;
  total: number;
};

@Component({
  components: {Selection2, UiButton}
})
export default class InstallModModal extends Vue {
  @Prop() mod!: Mod;
  @Prop() instance!: InstanceJson;
  @Prop() mcVersion!: string;
  @Prop() modLoader!: string;
  
  static emptyProgress = {
    percentage: 0,
    speed: 0,
    current: 0,
    total: 0,
  };
  
  @Prop() open!: boolean;
  @Emit() close() {}
  @Emit() installed() {}

  installing = false;
  finishedInstalling = false;
  selectedVersion: string | null = null;
  
  installProgress: InstallProgress = InstallModModal.emptyProgress;
  wsReqId = "";
  
  mounted() {
    emitter.on('ws.message', this.onInstallMessage);
  }

  destroyed() {
    // Stop listening to events!
    emitter.off('ws.message', this.onInstallMessage);
  }
  
  onInstallMessage(data: any) {
    if (!this.installing || this.wsReqId !== data.requestId) {
      return;
    }

    // Handle progress
    if (data.type === 'instanceInstallModProgress') {
      this.installProgress = {
        percentage: data.overallPrecentage,
        speed: data.speed,
        current: data.currentBytes,
        total: data.overallBytes,
      };
    }

    // Handle completion
    if (data.type === 'instanceInstallModReply') {
      this.wsReqId = "";
      this.installing = false;
      this.installProgress = InstallModModal.emptyProgress;
      this.finishedInstalling = true;
      this.installed()
    }
  }

  async installMod() {
    if (!this.selectedVersion) {
      return;
    }

    this.installing = true;
    const result = await sendMessage("instanceInstallMod", {
      uuid: this.instance?.uuid,
      modId: this.mod.id,
      versionId: parseInt(this.selectedVersion, 10),
    })

    this.wsReqId = result.messageId;
    this.selectedVersion = null;
  }
  
  get options() {
    return this.mod.versions
        .filter(e => e.targets.findIndex(a => a.type === 'modloader' && compatibleCrossLoaderPlatforms(this.mcVersion, this.modLoader).includes(a.name)) !== -1)
        .filter(e => e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === this.mcVersion) !== -1)
        .sort((a, b) => b.id - a.id)
        .map((e) => ({
            value: e.id,
            label: e.name,
            badge: {
              text: e.type,
              color: getColorForReleaseType(e.type),
            },
            meta: prettyByteFormat(e.size),
          })) ?? []
  }
}
</script>

<style lang="scss" scoped>

</style>