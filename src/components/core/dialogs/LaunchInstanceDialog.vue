<template>
  <modal :permanent="true" :open="launchingStatus !== null" :title="title" :sub-title="subtitle">
    <template v-if="launchingStatus">
      <loader v-if="!launchingStatus.error" :title="launchingStatus?.starting ? 'Starting...' : 'Logging in...'" />
      <div v-else>
        <p class="bold text-lg text-red-600">Instance failed to launch!</p>
        <code class="whitespace-pre-wrap" v-if="launchingStatus.error">
          {{launchingStatus.error}}
        </code>
      </div>
    </template>
    <span v-else>This shouldn't be possible</span>
  </modal>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {LaunchingStatus} from '@/core/state/misc/runningState';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import Loader from '@/components/atoms/Loader.vue';

@Component({
  components: {Loader}
})
export default class LaunchInstanceDialog extends Vue {
  @Getter("instances", ns("v2/instances")) public instances!: (SugaredInstanceJson | InstanceJson)[];
  @Getter("launchingStatus", ns("v2/running")) public launchingStatus!: LaunchingStatus | null;
  
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