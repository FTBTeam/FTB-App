<template>
  <div class="px-6 py-4" v-if="isLoaded">
    <!-- <div class="flex flex-row items-center w-full mt-4">
      <h1 v-if="recentlyPlayed.length >= 1" @click="changeTab('recentlyPlayed')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'recentlyPlayed' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Recently Played</h1>
      <h1 @click="changeTab('featuredPacks')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'featuredPacks' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Featured Packs</h1>
      <h1 v-if="serverListState.servers !== undefined" @click="changeTab('featuredServers')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'featuredServers' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Featured Servers</h1>
    </div> -->
    <div class="flex flex-col" v-if="recentlyPlayed.length >= 1" key="recentlyPlayed">
      <h1 class="text-2xl mb-2">Recently Played</h1>
      <div class="mod-pack-grid mb-4">
        <pack-card
          v-for="modpack in recentlyPlayed"
          :key="modpack.uuid"
          :versions="modpack.versions"
          :art="modpack.art"
          :installed="true"
          :version="modpack.version"
          :name="modpack.name"
          :authors="modpack.authors"
          :instance="modpack"
          :instanceID="modpack.uuid"
          :description="
            getModpack(modpack.id) !== undefined ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'
          "
          :tags="getModpack(modpack.id) !== undefined ? getModpack(modpack.id).tags : []"
          :kind="modpack.kind"
        ></pack-card>
      </div>
    </div>
    <div class="flex flex-col" key="featuredPacks">
      <h1 class="text-2xl mr-4">Featured Modpacks</h1>
      <div class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch" appear>
        <pack-card-list
          v-for="modpack in modpacks.featuredPacks.slice(0, cardsToShow)"
          :key="modpack.id"
          :packID="modpack.id"
          :versions="modpack.versions"
          :art="modpack.art"
          :installed="false"
          :minecraft="'1.7.10'"
          :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
          :versionID="modpack.versions[0].id"
          :name="modpack.name"
          :authors="modpack.authors"
          :description="modpack.synopsis"
          :tags="modpack.tags"
          :kind="modpack.kind"
          >{{ modpack.id }}</pack-card-list
        >
      </div>
    </div>
    <!-- <div class="flex flex-col" v-if="currentTab === 'featuredServers'" key="featuredServers">
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
    </div> -->
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <strong>Loading</strong>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import PackCard from '@/components/packs/PackCard.vue';
import PackCardList from '@/components/packs/PackCardList.vue';
import ServerCard from '@/components/ServerCard.vue';
import { State, Action } from 'vuex-class';
import { ModpackState, ModPack } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';
import { ServersState } from '@/modules/servers/types';
import FtbButton from '@/components/FTBButton.vue';
const namespace: string = 'modpacks';

@Component({
  components: {
    FtbButton,
    PackCardWrapper,
    ServerCard,
    PackCard,
    PackCardList,
  },
})
export default class Home extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('loadFeaturedPacks', { namespace }) public loadFeaturedPacks: any;
  @Action('loadAllPacks', { namespace }) public loadAllPacks: any;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: (id: number) => Promise<ModPack>;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: (id: number) => Promise<ModPack>;
  @State('servers') public serverListState!: ServersState;
  @Action('fetchFeaturedServers', { namespace: 'servers' }) public fetchFeaturedServers!: any;

  public currentTab = this.recentlyPlayed.length >= 1 ? 'recentlyPlayed' : 'featuredPacks';
  private cardsToShow = 10;
  private isLoaded: boolean = false;

  @Watch('modpacks', { deep: true })
  public async onModpacksChange(newVal: ModpackState, oldVal: ModpackState) {
    if (JSON.stringify(newVal.installedPacks) !== JSON.stringify(oldVal.installedPacks)) {
      this.isLoaded = false;
      try {
        await Promise.all(
          newVal.installedPacks.map(async instance => {
            const pack =
              instance.packType == 0 ? await this.fetchModpack(instance.id) : await this.fetchCursepack(instance.id);
            return pack;
          }),
        );
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  public getModpack(id: number): ModPack | undefined {
    return this.modpacks?.packsCache[id];
  }

  private async mounted() {
    // this.fetchFeaturedServers();
    this.currentTab = this.recentlyPlayed.length >= 1 ? 'recentlyPlayed' : 'featuredPacks';
    if (this.modpacks == null || this.modpacks.featuredPacks.length <= 0) {
      await this.loadAllPacks();
      await this.loadFeaturedPacks();
    }
    if (this.modpacks) {
      this.isLoaded = false;
      try {
        await Promise.all(
          this.modpacks.installedPacks.map(async instance => {
            const pack =
              instance.packType == 0 ? await this.fetchModpack(instance.id) : await this.fetchCursepack(instance.id);
            return pack;
          }),
        );
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  get recentlyPlayed() {
    return this.modpacks !== undefined
      ? this.modpacks.installedPacks
          .sort((a, b) => {
            return b.lastPlayed - a.lastPlayed;
          })
          .slice(0, 6)
      : [];
  }
}
</script>

<style lang="scss" scoped>
.mod-pack-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, 150px);
  gap: 0.92rem;
}
</style>
