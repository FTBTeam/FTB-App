<template>
  <transition name="transition-fade">
    <div class="app-install" v-if="currentInstall || installQueue.length > 0">
      <div class="btn flex gap-2 items-center bg-green-500" @click="open = !open">
        <font-awesome-icon icon="circle-notch" spin />
        Downloading
      </div>

      <transition name="transition-fade-and-up">
        <div class="dropdown" v-if="open">
          <div class="group" v-if="currentInstall">
            <div class="name">Installing</div>
            <install-queue-row class="row" :item="currentInstall" :is-install="true" />
          </div>
          <div class="group" v-if="onlyQueue.length > 0">
            <div class="name">Queue</div>
            <install-queue-row class="row" v-for="(item, index) in onlyQueue" :key="index" :is-next="index === 0" :item="item" :is-install="false" />
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {InstallRequest, InstallStatus} from '@/core/controllers/InstanceInstallController';
import InstallQueueRow from '@/components/core/modpack/InstallQueue/InstallQueueRow.vue';

@Component({
  components: {InstallQueueRow}
})
export default class InstallQueue extends Vue {
  @Getter("currentInstall", ns("v2/install")) currentInstall!: InstallStatus | null;
  @Getter("installQueue", ns("v2/install")) installQueue!: InstallRequest[];
  
  open = false;
  
  @Watch('currentInstall')
  onCurrentInstallChanged() {
    if (this.installQueue.length === 0 && this.currentInstall) {
      this.open = true;
    }
  }
  
  mounted() {
    document.addEventListener('click', this.close);
  }
  
  destroyed() {
  document.removeEventListener('click', this.close);
  }
  
  close(event: any) {
    if (!this.$el.contains(event.target as any)) {
      this.open = false;
    }
  }
  
  get onlyQueue() {
    return this.installQueue.filter(e => e.uuid !== this.currentInstall?.request.uuid)
  }
}
</script>

<style lang="scss" scoped>
.group {
  &:not(:last-child) {
    margin-bottom: .5rem;
  }
  
  .name {
    font-weight: 600;
    font-size: .8rem;
    margin-bottom: 0.35rem;
  }
  
  .row:not(:last-child) {
    margin-bottom: 0.35rem;
  }
}

.app-install {
  position: relative;

  .btn {
    cursor: pointer;
    padding: .5rem .75rem;
    border: 1px solid rgba(white, .2);
    border-radius: 3px;
  }

  .dropdown {
    position: absolute;
    top: 120%;
    right: 0;
    z-index: 100;
    width: 510px;
    background-color: var(--color-background);
    padding: 1rem;
    border-radius: 5px;
    box-shadow: 0 5px 0.5rem rgb(0 0 0 / 20%);
    border: 1px solid #1a1a1a;
  }
}
</style>