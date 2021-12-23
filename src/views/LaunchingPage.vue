<template>
  <div class="pack-loading">
    <!--    <div class="background" :style="{ backgroundImage: `url(${art})` }"></div>-->
    <header class="flex">
      <div class="art rounded shadow mr-8">
        <img :src="artSquare" width="135" alt="" />
      </div>

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">{{ preLaunch ? 'Initializing' : 'Starting' }} {{ instance.name }}</h3>

        <div class="loading-area" v-if="!loading && currentModpack !== null">
          <div class="bar mb-4" v-for="(bar, index) in bars" :key="index">
            <span class="mb-2 block text-sm">{{ bar.message }}</span>
            <progress-bar :progress="bar.step / bar.steps" />
          </div>
          <div class="bar">
            <span class="mb-2 mt-2 text-sm block">
              <font-awesome-icon icon="circle-notch" class="mr-2" spin />
              Total progress
            </span>
            <progress-bar :progress="2 / 10" />
          </div>
        </div>
      </div>
    </header>

    <div class="logs flex justify-between items-center" :class="{ 'dark-mode': darkMode }">
      <h3 class="font-bold text-lg">Log</h3>
      <div class="buttons flex items-center ">
        <ftb-button
          @click="cancelLoading"
          class="transition ease-in-out duration-200 py-1 px-4 text-xs mr-4 border-red-600 border border-solid hover:bg-red-600 hover:text-white"
        >
          <font-awesome-icon icon="skull-crossbones" class="mr-2" />
          Kill instance
        </ftb-button>
        <ftb-button
          class="transition ease-in-out duration-200 text-xs border border-solid px-2 py-1 mr-4 hover:bg-green-600 hover:text-white hover:border-green-600"
          :class="{ 'border-black': !darkMode, 'border-white': darkMode }"
        >
          <font-awesome-icon icon="upload" class="mr-2" />
          Upload logs
        </ftb-button>
        <div class="color cursor-pointer" @click="darkMode = !darkMode">
          <font-awesome-icon :icon="['fas', darkMode ? 'sun' : 'moon']" />
        </div>
      </div>
    </div>

    <div class="log-contents text-sm" :class="{ 'dark-mode': darkMode }">
      <div class="log-item" v-for="n in 100">[debug][{{ n }}]: AHHHH</div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModpackState } from '@/modules/modpacks/types';
import { Action, State } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import ServerCard from '@/components/organisms/ServerCard.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import platform from '@/utils/interface/electron-overwolf';
import ProgressBar from '@/components/atoms/ProgressBar.vue';

@Component({
  name: 'LaunchingPage',
  components: {
    'ftb-toggle': FTBToggle,
    InstallModal,
    FTBModal,
    'message-modal': MessageModal,
    ServerCard,
    ProgressBar,
  },
})
export default class LaunchingPage extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('sendMessage') public sendMessage!: any;

  loading = false;
  preLaunch = true;
  platform = platform;

  darkMode = true;

  public cancelLoading() {
    this.sendMessage({
      payload: {
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'yeet',
      },
    });
  }

  public restoreLoading() {
    this.sendMessage({
      payload: {
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'show',
      },
    });
  }

  public async mounted() {
    if (this.instance == null) {
      return null;
    }

    await this.fetchModpack(this.instance?.id);
    if (this.modpacks.packsCache[this.instance?.id] !== undefined) {
      this.loading = false;
    }
  }

  get instance() {
    if (this.modpacks === null) {
      return null;
    }

    return this.modpacks.installedPacks.filter(pack => pack.uuid === this.$route.query.uuid)[0];
  }

  get limitedTags() {
    if (this.currentModpack && this.currentModpack.tags) {
      return this.currentModpack.tags.slice(0, 5);
    } else {
      return [];
    }
  }

  get bars() {
    if (this.modpacks === undefined || this.modpacks === null) {
      return [];
    }
    if (this.modpacks.launchProgress === null) {
      return [];
    }

    return (
      this.modpacks.launchProgress?.filter(b => b.steps !== 1) || [
        {
          title: 'Loading...',
          message: 'Completed deep scan of ftb-ranks-1605.1.4-build.12-forge.jar',
          steps: 100,
          step: 50,
        },
      ]
    );
  }

  get currentModpack() {
    if (this.instance == null) {
      return null;
    }
    const id: number = this.instance.id;
    if (this.modpacks.packsCache[id] === undefined) {
      return null;
    }
    return this.modpacks.packsCache[id];
  }

  get art() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    const arts = this.currentModpack.art.filter(art => art.type === 'splash');
    return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  }

  get artSquare() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    const arts = this.currentModpack.art.filter(art => art.type === 'square');
    return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  }
}
</script>

<style lang="scss">
.pack-loading {
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  max-height: 100%;
  z-index: 1;

  > header {
    padding: 2rem;
  }

  .background {
    position: absolute;
    height: 200px;
    width: 100%;
    top: 0;
    left: 0;
    z-index: -1;

    &::before,
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: -1;
    }

    &::before {
      opacity: 0.5;
      background: black;
    }

    &::after {
      background: linear-gradient(0deg, rgba(black, 1) 25%, rgba(black, 0) 100%);
    }
  }

  .logs {
    padding: 1rem;
    background-color: black;

    header {
      padding: 1rem 1rem 0 1rem;
    }
  }

  > .buttons {
    background-color: black;
    justify-content: flex-end;
    padding: 1rem 1rem 0 1rem;
  }

  .log-contents {
    flex: 1;
    display: flex;
    background-color: black;
    flex-direction: column-reverse;
    padding: 1rem;
    overflow: auto;
    font-family: 'Consolas', 'Courier New', Courier, monospace;

    &::-webkit-scrollbar-track {
      background: transparent;
      z-index: 10;
    }

    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
      border-radius: 150px;
      z-index: 10;
    }
  }

  .logs,
  .log-contents {
    transition: 0.25s ease-in-out background-color, color 0.25s ease-in-out;
    background-color: white;
    color: #24292e;
    &.dark-mode {
      background-color: black;
      color: white;
    }
  }
}

.update-bar {
  font-weight: 700;
  margin-bottom: 1rem;
}
</style>
