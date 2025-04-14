<script lang="ts" setup>
// import UiButton from '@/components/ui/UiButton.vue';
// import Selection2 from '@/components/ui/Selection2.vue';
// import {emitter, getColorForReleaseType, prettyByteFormat} from '@/utils';
// import {sendMessage} from '@/core/websockets/websocketsApi';
// import {Mod} from '@/types';
// import {BaseData, InstanceJson, OperationProgressUpdateData} from '@/core/@types/javaApi';
// import {compatibleCrossLoaderPlatforms} from '@/utils/helpers/packHelpers';
//
// type InstallProgress = {
//   percentage: number;
//   speed: number;
//   current: number;
//   total: number;
// };
//
// @Component({
//   components: {Selection2, UiButton},
//   methods: {prettyByteFormat},
// })
// export default class InstallModModal extends Vue {
//   @Prop() mod!: Mod;
//   @Prop() instance!: InstanceJson;
//   @Prop() mcVersion!: string;
//   @Prop() modLoader!: string;
//
//   static emptyProgress = {
//     percentage: 0,
//     speed: 0,
//     current: 0,
//     total: 0,
//   };
//
//   @Prop() open!: boolean;
//   @Emit() close() {
//     this.installProgress = InstallModModal.emptyProgress;
//     this.installing = false;
//     this.finishedInstalling = false;
//     this.selectedVersion = null;
//   }
//
//   @Emit() installed() {}
//
//   installing = false;
//   finishedInstalling = false;
//   selectedVersion: string | null = null;
//
//   installProgress: InstallProgress = InstallModModal.emptyProgress;
//   wsReqId = "";
//
//   @Watch('open')
//   onOpen(newVal: boolean) {
//     if (!newVal) return;
//
//     this.selectedVersion = this.options[0]?.value as any;
//   }
//
//   mounted() {
//     emitter.on('ws.message', this.onInstallMessage);
//   }
//
//   destroyed() {
//     // Stop listening to events!
//     emitter.off('ws.message', this.onInstallMessage);
//   }
//
//   onInstallMessage(data: BaseData & any) {
//     if (data.type !== "operationUpdate" && data.type !== "instanceInstallModReply") {
//       return;
//     }
//
//     if (data.type === 'instanceInstallModReply') {
//       this.wsReqId = "";
//       this.installing = false;
//       this.installProgress = InstallModModal.emptyProgress;
//       this.finishedInstalling = true;
//       this.installed()
//       return;
//     }
//
//     // Handle progress
//     if (data.type === 'operationUpdate') {
//       const typedData = data as OperationProgressUpdateData;
//       if (typedData.metadata.instance !== this.instance.uuid) {
//         return;
//       }
//
//       this.installProgress = {
//         percentage: typedData.percent,
//         speed: typedData.speed,
//         current: typedData.bytes,
//         total: typedData.totalBytes,
//       };
//     }
//   }
//
//   async installMod() {
//     if (!this.selectedVersion) {
//       return;
//     }
//
//     this.installing = true;
//     const result = await sendMessage("instanceInstallMod", {
//       uuid: this.instance?.uuid,
//       modId: this.mod.id,
//       versionId: parseInt(this.selectedVersion, 10),
//     })
//
//     this.wsReqId = result.messageId;
//     this.selectedVersion = null;
//   }
//
//   get options() {
//     const crossLoaderPlatforms = compatibleCrossLoaderPlatforms(this.mcVersion, this.modLoader);
//     return this.mod.versions
//       .filter(e => e.targets.findIndex(a => a.type === 'modloader' && crossLoaderPlatforms.includes(a.name)) !== -1)
//       .filter(e => e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === this.mcVersion) !== -1)
//       .sort((a, b) => b.id - a.id)
//       .map((e) => ({
//         value: e.id,
//         label: e.name,
//         badge: {
//           text: e.type,
//           color: getColorForReleaseType(e.type),
//         },
//         meta: prettyByteFormat(e.size),
//       })) ?? []
//   }
// }
</script>

<template>
  <div>
<!--  // TODO: [port] fixme  -->
    TOOD: Add me back in
  </div>
<!--   <modal :open="open" @closed="close" :close-on-background-click="!installing"-->
<!--          :title="mod ? mod.name : 'Loading...'" :sub-title="!installing && finishedInstalling ? 'Installed!' : `Select the version you want to install`" :external-contents="true"-->
<!--   >-->
<!--     <modal-body v-if="mod">-->
<!--       <div class="py-6" v-if="!installing && !finishedInstalling">-->
<!--         <div class="flex gap-4 mb-4">-->
<!--           <font-awesome-icon icon="download" size="xl" />-->
<!--           <b class="text-lg block">Install {{ mod.name }}</b>-->
<!--         </div>-->
<!--         -->
<!--         <selection2-->
<!--           label="Select mod version"-->
<!--           v-model="selectedVersion"-->
<!--           :options="options"-->
<!--         />-->
<!--       </div>-->

<!--       <div v-if="!installing && !finishedInstalling" class="pt-8">-->
<!--         <div class="flex items-center gap-4 mb-6">-->
<!--           <img src="../../assets/curse-logo.svg" alt="CurseForge logo" width="40" />-->
<!--           <b class="text-lg block">About mod installs</b>-->
<!--         </div>-->
<!--         <hr class="curse-border block mb-4" />-->
<!--         <p class="text-muted mb-4">🎉 Each mod install from the FTB App directly supports the mod developers through the CurseForge reward system!</p>-->
<!--         <b class="block mb-2">Dependencies</b>-->
<!--         <p class="mb-2 text-muted">If the mod depends on other mods on the CurseForge platform, the app will try and resolve these dependencies for you and install them as well.</p>-->
<!--         <p class="text-muted">Sometimes this does not work and you'll have to install the dependencies manually.</p>-->
<!--       </div>-->

<!--       <div class="installing mt-6 mb-4" v-if="!finishedInstalling && installing">-->
<!--         <div class="progress font-bold"><font-awesome-icon icon="spinner" spin class="mr-2" /> Installing</div>-->
<!--         <div class="stats">-->
<!--           <div class="stat">-->
<!--             <div class="text">Progress</div>-->
<!--             <div class="value">{{ installProgress.percentage }}%</div>-->
<!--           </div>-->
<!--         </div>-->
<!--       </div>-->

<!--       <p v-if="!installing && finishedInstalling">-->
<!--         <span class="block">{{ mod.name }} has been installed!</span>-->
<!--       </p>-->
<!--     </modal-body>-->

<!--     <modal-footer>-->
<!--       <div class="flex justify-end gap-4" v-if="!installing || !finishedInstalling">-->
<!--         <ui-button type="success" icon="download" v-if="!installing && !finishedInstalling" :disabled="!selectedVersion" @click="installMod">Install</ui-button>-->
<!--         <ui-button type="primary" @click="close" icon="check" v-if="finishedInstalling">Close</ui-button>-->
<!--       </div>-->
<!--     </modal-footer>-->
<!--   </modal>-->
</template>