<template>
  <div class="px-6 py-4" v-if="isLoaded">
    <div class="flex flex-col" v-if="recentlyPlayed.length >= 1" key="recentlyPlayed">
      <h1 class="text-2xl mb-2">Recently Played</h1>
      <div class="mod-pack-grid mb-4">
        <pack-card
          v-for="modpack in recentlyPlayed"
          class="pack-card-item"
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
        />
      </div>
    </div>
    <div class="flex flex-col" key="featuredPacks">
      <h1 class="text-2xl mb-4 mt-2">Featured Modpacks</h1>
      <div class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch">
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
  </div>
  <div class="loading-screen" v-else>
    <Loading2 />
    <div class="packs-to-load" v-if="modpacks.packsLoaded">
      <span>{{ modpacks.packsLoaded }} / {{ modpacks.packsToLoad }}</span> Modpacks Loaded
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import PackCardWrapper from '@/components/organisms/packs/PackCardWrapper.vue';
import PackCard from '@/components/organisms/packs/PackCard.vue';
import PackCardList from '@/components/organisms/packs/PackCardList.vue';
import ServerCard from '@/components/organisms/ServerCard.vue';
import { Action, State } from 'vuex-class';
import { ModPack, ModpackState } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';
import { ServersState } from '@/modules/servers/types';
import FtbButton from '@/components/atoms/input/FTBButton.vue';
import Loading from '@/components/atoms/Loading.vue';
import Loading2 from "@/components/atoms/Loading2.vue";

const namespace: string = 'modpacks';

@Component({
  components: {
    Loading2,
    Loading,
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
          newVal.installedPacks.map(async (instance) => {
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

  async mounted() {
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
          this.modpacks.installedPacks.map(async (instance) => {
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
  overflow-x: auto;
  display: flex;
  justify-content: flex-start;
  padding-bottom: 1rem;

  .pack-card-item {
    // Packcard needs rewriting, it's being restricted by something :/
    min-width: 150px;
    max-width: 150px;
    width: 150px;

    &:not(:last-child) {
      margin-right: 1rem;
    }
  }
}

.loading-screen {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  
  .packs-to-load {
    background: black;
    box-shadow: 0 0 15px rgba(black, .15);
    padding: .4rem 1rem;
    border-radius: 3px;
    
    span {
      font-weight: bold;
      margin-right: 1rem;
    }
  }
}
</style>
