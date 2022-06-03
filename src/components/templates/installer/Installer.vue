<template>
  <modal
    :open="installer"
    :title="`${
      error
        ? 'Install failed'
        : completed
        ? `Modpack installed!`
        : `${installer && installer.meta && installer.meta.isUpdate ? 'Updating' : 'Installing'} Modpack`
    }`"
    :subTitle="`${error ? 'Somethings gone wrong during the install...' : ''}`"
    @closed="closed"
  >
    <div class="installer" v-if="installer">
      <div class="about">
        <img :src="`${art}`" class="logo" alt="Pack artwork" />
        <div class="info">
          <div class="name text-lg font-bold">{{ installer.meta.name }}</div>
          <div class="version">{{ installer.meta.version }}</div>
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
        <ftb-button class="py-3 px-4 mt-10 text-center w-full" color="primary" @click="() => navigateToInstance()"
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
import { InstallerState } from '@/modules/app/appStore.types';
import placeholderArt from '@/assets/placeholder_art.png';
import { wsTimeoutWrapper } from '@/utils';
import { Instance } from '@/modules/modpacks/types';

@Component({
  components: {
    ProgressBar,
  },
})
export default class Installer extends Vue {
  @Action('sendMessage') public sendMessage!: any;
  @Action('storeInstalledPack', { namespace: 'modpacks' }) public storePack!: (payload: {
    pack: Instance;
    type: 'instance' | 'cloudInstance';
  }) => void;

  @Action('updatePackInStore', { namespace: 'modpacks' }) public updatePackInStore!: (pack: Instance) => void;

  @Getter('installer', { namespace: 'app' }) public installer!: InstallerState | null;
  @Action('clearInstaller', { namespace: 'app' }) public clearInstaller!: () => void;

  tidyNameMap = new Map([
    ['init', 'Setting up installer'],
    ['prepare', 'Preparing instance'],
    ['modloader', 'Installing the Modloader'],
    ['downloads', 'Downloading contents'],
    ['finished', 'Finished'],
  ]);

  packName = '';
  packVersion = '';

  files = null;
  filesComplete = 0;
  completed = false;
  percentage = '0.00';
  status = '';
  completedUuid = null;
  error = '';

  mounted() {
    eventBus.$on('ws.message', (data: any) => {
      if (!this.installer) {
        if (data.type === 'installInstanceDataReply' && (data.status === 'error' || data.status === 'prepare_error')) {
          // Failsafe to ensure everything is reset if we see an error even after the installer has been closed.
          this.closed();
        }
        return;
      }

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

  async onUpdate(data: any) {
    if (data.status === 'init' && this.error) {
      this.error = '';
    }

    if (data.status === 'error' || data.status === 'prepare_error') {
      this.error = data.message;
      this.completed = true;
      this.percentage = '100';
      return;
    }

    if (data.status === 'success') {
      if (this.installer?.meta.isUpdate) {
        this.updatePackInStore(data.instanceData);
      } else {
        this.storePack({
          pack: data.instanceData,
          type: 'instance',
        });
      }

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
    this.closed();
    this.$router
      .push({
        name: 'instancepage',
        query: { uuid: this.completedUuid },
      })
      .catch(() => {});
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

  async closed() {
    this.cleanUp();
    this.clearInstaller();

    if (!this.completed && !this.error) {
      wsTimeoutWrapper({
        type: 'cancelInstallInstance',
        uuid: '-1',
      }).catch(() => {});
    }
  }

  // public retry() {
  //   this.closed();
  //
  //   this.sendMessage({
  //     payload: { type: 'uninstallInstance', uuid: this.installer?.pack.uuid },
  //     callback: (data: any) => {
  //       this.sendMessage({
  //         payload: { type: 'installedInstances', refresh: true },
  //         callback: (data: any) => {
  //           this.storePacks(data);
  //           // run install pack method again
  //         },
  //       });
  //     },
  //   });
  // }

  destroyed() {
    eventBus.$off('ws.message');
  }

  get art() {
    return this.installer?.meta.art ? this.installer?.meta.art : placeholderArt;
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
