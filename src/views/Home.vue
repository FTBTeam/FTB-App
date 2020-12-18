<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5" style="margin-bottom: 60px;" v-if="isLoaded">
    <div class="absolute z-50 display-mode-switcher ml-auto top-0" style="right: 118px; -webkit-app-region: no-drag;">
        <div :class="`cursor-pointer px-2 py-1 ${settingsState.settings.listMode === true || settingsState.settings.listMode === 'true' ? 'bg-gray-600' : 'bg-background-lighten'}`" @click="changeToList">
          <font-awesome-icon icon="list" class="cursor-pointer"></font-awesome-icon>
        </div>
        <div :class="`cursor-pointer px-2 py-1 ${settingsState.settings.listMode === false || settingsState.settings.listMode === 'false' ? 'bg-gray-600' : 'bg-background-lighten'}`"  @click="changeToGrid">
          <font-awesome-icon icon="th" class="cursor-pointer"></font-awesome-icon>
        </div>
    </div>
    <div class="flex flex-row items-center w-full mt-4">
      <h1 v-if="recentlyPlayed.length >= 1" @click="changeTab('recentlyPlayed')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'recentlyPlayed' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Recently Played</h1>
      <h1 @click="changeTab('featuredPacks')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'featuredPacks' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Featured Packs</h1>
      <h1 v-if="serverListState.servers !== undefined" @click="changeTab('featuredServers')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'featuredServers' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Featured Servers</h1>
    </div>
    <div class="flex flex-col" v-if="recentlyPlayed.length >= 1 && currentTab === 'recentlyPlayed'" key="recentlyPlayed">
      <transition-group
        name="list"
        tag="div"
        class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch"
        appear
      >
        <pack-card-wrapper
          v-for="modpack in recentlyPlayed"
          :list-mode="settingsState.settings.listMode"
          :key="modpack.uuid"
          :versions="modpack.versions"
          :art="modpack.art"
          :installed="true"
          :version="modpack.version"
          :name="modpack.name"
          :authors="modpack.authors"
          :instance="modpack"
          :instanceID="modpack.uuid"
          :description="getModpack(modpack.id) !== undefined ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'"
          :tags="getModpack(modpack.id) !== undefined ? getModpack(modpack.id).tags : []"
          :kind="modpack.kind"
        ></pack-card-wrapper>
      </transition-group>
    </div>
    <div class="flex flex-col" v-if="currentTab === 'featuredPacks'" key="featuredPacks">
      <transition-group
        name="list"
        tag="div"
        class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch"
        appear
      >
        <pack-card-wrapper
          v-for="modpack in modpacks.featuredPacks.slice(0,cardsToShow)"
          :key="modpack.id"
          :list-mode="settingsState.settings.listMode"
          :packID="modpack.id"
          :versions="modpack.versions"
          :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
          :installed="false"
          :minecraft="'1.7.10'"
          :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
          :versionID="modpack.versions[0].id"
          :name="modpack.name"
          :authors="modpack.authors"
          :description="modpack.synopsis"
          :tags="modpack.tags"
          :kind="modpack.kind"
        >{{modpack.id}}</pack-card-wrapper>
      </transition-group>
    </div>
    <div class="flex flex-col" v-if="currentTab === 'featuredServers'" key="featuredServers">
      <transition-group
        name="list"
        tag="div"
        class="flex pt-1 flex-wrap flex-grow items-stretch"
        appear
      >
        <div v-if="serverListState.servers['featured'].length > 0" :key="'servers'">
          <server-card v-if="serverListState.servers !== null" v-for="server in serverListState.servers['featured']" :key="server.id" :server="server"></server-card>
        </div>
        <div class="flex flex-1 pt-1 flex-wrap overflow-x-auto justify-center flex-col items-center" :key="'no-servers'" v-else>
          <font-awesome-icon icon="server" style="font-size: 25vh"></font-awesome-icon>
          <h1 class="text-5xl">Oh no!</h1>
          <span>It doesn't looks like there are any featured MineTogether servers</span>
          <br/>
          <span>If you are a server owner and would like to have your server featured,</span>
          <span>you can find more information by clicking the button below</span>
          <a href="https://feed-the-beast.com/featuredServers" target="_blank"><ftb-button class="py-2 px-4 my-2" color="info" css-class="text-center text-l">Become a featured server</ftb-button></a>
        </div>
      </transition-group>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <strong>Loading</strong>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch} from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import ServerCard from '@/components/ServerCard.vue';
import { asyncForEach } from '@/utils';
import { State, Action} from 'vuex-class';
import { ModpackState, ModPack } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';
import { settings } from 'cluster';
import {ServersState} from '@/modules/servers/types';
import FtbButton from "@/components/FTBButton.vue";

const namespace: string = 'modpacks';

@Component({
  components: {
    FtbButton,
    PackCardWrapper,
    ServerCard,
  },
})
export default class Home extends Vue {

  get recentlyPlayed() {
    return this.modpacks !== undefined
      ? this.modpacks.installedPacks
          .sort((a, b) => {
            return b.lastPlayed - a.lastPlayed;
          })
          .slice(0, 4)
      : [];
  }
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', {namespace: 'settings'}) public saveSettings: any;
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('loadFeaturedPacks', { namespace }) public loadFeaturedPacks: any;
  @Action('loadAllPacks', { namespace }) public loadAllPacks: any;
  @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack!: (id: number) => Promise<ModPack>;
  @State('servers') public serverListState!: ServersState;
  @Action('fetchFeaturedServers', {namespace: 'servers'}) public fetchFeaturedServers!: any;
  public currentTab = this.recentlyPlayed.length >= 1 ? 'recentlyPlayed' : 'featuredPacks';

  private cardsToShow = 3;
  private isLoaded: boolean = false;

  @Watch('modpacks', {deep: true})
  public async onModpacksChange(newVal: ModpackState, oldVal: ModpackState) {
    if (JSON.stringify(newVal.installedPacks) !== JSON.stringify(oldVal.installedPacks)) {
      this.isLoaded = false;
      try {
        await Promise.all(newVal.installedPacks.map(async (instance) => {
          const pack = await this.fetchModpack(instance.id);
          return pack;
        }));
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  public changeToList() {
    const settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = true;
    this.saveSettings(settingsCopy);
  }

  public changeToGrid() {
    const settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = false;
    this.saveSettings(settingsCopy);
  }

  public getModpack(id: number): ModPack | undefined {
      return this.modpacks?.packsCache[id];
  }

    private changeTab(tab: string) {
        this.currentTab = tab;
    }

  private async mounted() {
    this.fetchFeaturedServers();
    this.currentTab = this.recentlyPlayed.length >= 1 ? 'recentlyPlayed' : 'featuredPacks';
    if (this.modpacks == null || this.modpacks.featuredPacks.length <= 0) {
      await this.loadAllPacks();
      await this.loadFeaturedPacks();
    }
    if (this.modpacks) {
      this.isLoaded = false;
      try {
        await Promise.all(this.modpacks.installedPacks.map(async (instance) => {
            const pack = await this.fetchModpack(instance.id);
            return pack;
        }));
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
    const cardSize =  this.settingsState.settings.packCardSize || 2;
    // @ts-ignore
    switch (parseInt(cardSize, 10)) {
      case 5:
        this.cardsToShow = 3;
        break;
      case 4:
        this.cardsToShow = 4;
        break;
      case 3:
        this.cardsToShow = 5;
        break;
      case 2:
        this.cardsToShow = 7;
        break;
      case 1:
        this.cardsToShow = 10;
        break;
      default:
        this.cardsToShow = 10;
        break;
    }
  }
}
</script>
<style lang="scss" scoped>
  .display-mode-switcher {
    display: flex;
    flex-direction: row-reverse;
    background-color: #313131;
  }
</style>
