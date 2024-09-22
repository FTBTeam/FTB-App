<template>
  <modal :permanent="!launchingStatus?.error" :open="launchingStatus !== null" :title="title" :sub-title="subtitle" @closed="() => clearLaunchingStatus()">
    <template v-if="launchingStatus">
      <loader v-if="!launchingStatus.error" :title="launchingStatus?.starting ? 'Starting...' : 'Logging in...'" />
      <div v-else>
        <p class="bold text-lg text-red-500 font-bold mb-3">Instance failed to launch!</p>
        
        <code class="whitespace-pre overflow-auto block bg-black p-2 rounded mb-6" style="max-height: 300px" v-if="launchingStatus.error">
          {{launchingStatus.error}}
        </code>
        
        <ui-button :full-width="true" @click="() => clearLaunchingStatus()">Close</ui-button>
      </div>
    </template>
    <span v-else>This shouldn't be possible</span>
  </modal>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {LaunchingStatus} from '@/core/state/misc/runningState';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import Loader from '@/components/atoms/Loader.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import store from '@/modules/store';

@Component({
  components: {UiButton, Loader}
})
export default class LaunchInstanceDialog extends Vue {
  @Getter("instances", ns("v2/instances")) public instances!: (SugaredInstanceJson | InstanceJson)[];
  @Getter("launchingStatus", ns("v2/running")) public launchingStatus!: LaunchingStatus | null;
  
  @Action("clearLaunchingStatus", ns("v2/running")) public clearStatus!: () => void;
  
  async clearLaunchingStatus() {
    console.log("Cleaning launching status");
    await store.dispatch('v2/running/stopped', this.instance?.uuid);
    this.clearStatus();
  }
  
  get title() {
    if (this.instance) {
      return `Launching ${this.instance.name}`;
    }
    
    return "Launching Instance";
  }
  
  get subtitle() {
    if (!this.launchingStatus) {
      return "Launching...";
    }
    
    if (this.launchingStatus.error) {
      return "Error launching instance";
    }
    
    return this.launchingStatus.step;
  }
  
  get instance() {
    if (!this.launchingStatus) return null;
    return this.instances.find(i => i.uuid === this.launchingStatus?.uuid);
  }
}
</script>

<style lang="scss" scoped>

</style>