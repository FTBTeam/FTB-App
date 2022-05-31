<template>
  <modal
    :open="installingPack"
    :title="`${error ? 'Install failed' : 'Installing Modpack'}`"
    :subTitle="`${error ? 'Somethings gone wrong during the install...' : ''}`"
    :permanent="!error"
    @closed="closed"
  >
    <div class="installer" v-if="installingPack">
      <div class="about">
        <img src="@/assets/placeholder_art.png" class="logo" alt="Pack artwork" />
        <div class="info">
          <div class="name text-lg font-bold">{{ installingPack.meta.name }}</div>
          <div class="version">{{ installingPack.meta.version }}</div>
        </div>
      </div>

      <template v-if="!completed">
        <div class="progress">
          <div class="current-step font-bold mb-4">
            <span>{{ tidyNameMap.has(status) ? tidyNameMap.get(status) : 'Preparing...' }}</span>
            <span class="text-lg">{{ percentage }}%</span>
          </div>
          <progress-bar :progress="percentage / 100" class="mb-4" />
        </div>

        <div class="install-info" v-if="files">
          <p>Downloaded files</p>
          <span>{{ filesComplete.toLocaleString() }}</span> /
          <span>{{ files ? files.toLocaleString() : 0 }}</span>
        </div>
      </template>
      <template v-else-if="completed && !error">
        <ftb-button
          class="py-2 px-4 mt-2 text-center float-right"
          color="primary"
          css-class="text-center text-l"
          @click="() => navigateToInstance()"
          >Go to Instance</ftb-button
        >
      </template>
      <template v-else>
        <message type="danger">
          {{ error }}
        </message>
      </template>
    </div>
  </modal>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import eventBus from '@/utils/event-bus';
import { Action, Getter } from 'vuex-class';
import { InstallingState } from '@/modules/app/appStore.types';

@Component({
  components: {
    ProgressBar,
  },
})
export default class Installer extends Vue {
  @Action('sendMessage') public sendMessage!: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;

  @Getter('installingPack', { namespace: 'app' }) public installingPack!: InstallingState | null;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallingState | null) => void;

  tidyNameMap = new Map([
    ['init', 'Setting up installer'],
    ['prepare', 'Preparing instance'],
    ['modloader', 'Installing the Modloader'],
    ['downloads', 'Downloading contents'],
    ['finished', 'Finished'],
  ]);

  packName = 'FTB Presents Stoneblock 2';
  packVersion = '1.2.0';

  files = null;
  filesComplete = 0;
  completed = false;
  percentage = '0.00';
  status = '';
  completedUuid = null;
  error = '';

  mounted() {
    eventBus.$on('ws.message', (data: any) => {
      if (
        data.type !== 'installInstanceDataReply' &&
        data.type !== 'installInstanceProgress' &&
        data.type !== 'install.filesEvent'
      ) {
        return;
      }

      // Actual install status update
      if (data.type === 'installInstanceDataReply') {
        this.onUpdate(data);
      }

      // Just install progress
      if (data.type === 'installInstanceProgress') {
        this.onProgressUpdate(data);
      }

      if (data.type === 'install.filesEvent') {
        this.onFilesEvent(data);
      }
    });
  }

  onUpdate(data: any) {
    if (data.status === 'error') {
      this.error = data.message;
      this.completed = true;
      this.percentage = '100';
      return;
    }

    if (data.status === 'success') {
      this.sendMessage({
        payload: { type: 'installedInstances', refresh: true },
        callback: (data: any) => {
          this.storePacks(data);
        },
      });
      this.completed = true;
      this.files = null;
      this.completedUuid = data.uuid;
      return;
    }

    if (data.status === 'files' && !this.files) {
      this.files = JSON.parse(data.message).length;
      return;
    }

    this.status = data.status;
  }

  onProgressUpdate(data: any) {
    this.status = data.currentStage.toLowerCase();
    this.percentage = data.overallPercentage.toFixed(2);
  }

  onFilesEvent(data: any) {
    if (!data.files) {
      return;
    }

    this.filesComplete += Object.keys(data.files).length ?? 0;
  }

  navigateToInstance() {
    this.$router.push({
      name: 'instancepage',
      query: { uuid: this.completedUuid },
    });

    this.cleanUp();
  }

  cleanUp() {
    this.completed = false;
    this.percentage = '0.00';
    this.files = null;
    this.packName = '';
    this.packVersion = '';
    this.status = '';
    this.filesComplete = 0;
  }

  closed() {
    this.cleanUp();
    this.installModpack(null);
  }

  destroyed() {
    eventBus.$off('ws.message');
  }
}
</script>

<style lang="scss" scoped>
.installer {
  padding-top: 0.5rem;

  .about {
    display: flex;
    align-items: center;
    margin-bottom: 1.5rem;

    .logo {
      width: 80px;
      margin-right: 1.5rem;
      border-radius: 10px;
    }
  }

  .current-step {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
  }

  .install-info {
    display: flex;
    align-items: center;
    justify-content: flex-end;

    p {
      margin-right: 0.3rem;
    }

    span {
      display: inline-block;
      background-color: rgba(black, 0.2);
      padding: 0.3rem 0.7rem;
      font-size: 0.875em;
      border-radius: 5px;
      margin: 0 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }
}
</style>
