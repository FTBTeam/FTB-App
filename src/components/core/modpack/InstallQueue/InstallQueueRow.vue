<template>
  <div class="install-query-row flex gap-4 items-center">
    <div class="name flex-1">
      {{request.name}}
    </div>
    <div class="status">
      <div class="install-status" v-if="status">
        {{status.stage}} - <span>{{ status.progress }}%</span> {{status.speed ? `(${(status.speed / 12500000).toFixed(2)} MB/s)` : ''}}
      </div>
      <div class="install-status" v-else>
        Queued <span class="inline-block" aria-label="Up next for download" data-balloon-pos="down-right" v-if="isNext">(Next)</span>
      </div>
    </div>
    <div class="actions">
      <div class="btn">
        <font-awesome-icon icon="times" @click="cancelInstall(item)" v-if="!cancelling" />
        <font-awesome-icon icon="circle-notch" spin v-else />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {InstallRequest, InstallStatus, instanceInstallController} from '@/core/controllers/InstanceInstallController';
import UiButton from '@/components/core/ui/UiButton.vue';

@Component({
  components: {UiButton},
})
export default class InstallQueueRow extends Vue {
  @Prop() item!: InstallRequest | InstallStatus;
  @Prop() isInstall!: boolean;
  @Prop({ default: false }) isNext!: boolean;

  cancelling = false;
  
  async cancelInstall(item: InstallRequest | InstallStatus) {
    if (this.cancelling) {
      return;
    }
    
    const uuid = this.isInstall ? (item as InstallStatus).request.uuid : (item as InstallRequest).uuid;

    this.cancelling = true;
    try {
      await instanceInstallController.cancelInstall(uuid, this.isInstall);
    } catch (e) {
      console.error(e);
    }
    this.cancelling = false;
  }
  
  get request(): InstallRequest {
    if (this.isInstall) {
      return (this.item as InstallStatus).request;
    }
    
    return this.item as InstallRequest;
  }
  
  get status(): InstallStatus | null {
    if (this.isInstall) {
      return this.item as InstallStatus
    }
    
    return null;
  }
}
</script>

<style lang="scss" scoped>
.install-query-row {
  padding: 0.45rem;
  border-bottom: 1px solid rgba(white, .1);
  font-size: 14px;
  font-weight: 500;
  background-color: rgba(white, .05);
  border-radius: 0.25rem;
  
  .name {
  }
  
  .status {
    span {
      color: white;
      font-weight: bold;
    }
  }
  
  .install-status {
    font-weight: 500;
  }
  
  .actions {
    .btn {
      cursor: pointer;
      background-color: rgba(white, .1);
      padding: 0 .35rem;
      border-radius: 3px;
      transition: background-color .2s ease-in-out;
      
      &:hover {
        background-color: var(--color-warning-button);
      }
    }
  }
}
</style>