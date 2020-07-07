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
    <div class="sm:mt-auto lg:mt-unset flex flex-col flex-grow" v-if="recentlyPlayed.length >= 1">
      <h1 class="text-2xl">Recently Played Packs</h1>
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
        ></pack-card-wrapper>
      </transition-group>
    </div>
    <div class="sm:mb-auto lg:mb-unset flex flex-col flex-grow">
      <h1 class="text-2xl">Featured Packs</h1>
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
        >{{modpack.id}}</pack-card-wrapper>
      </transition-group>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <!-- TODO: Add some kinda of loady spinner thing in the middle of the screen -->
    <strong>Loading</strong>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch} from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import { asyncForEach } from '@/utils';
import { State, Action} from 'vuex-class';
import { ModpackState, ModPack } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';
import { settings } from 'cluster';

const namespace: string = 'modpacks';

@Component({
  components: {
    PackCardWrapper,
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
  @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack!: (id: number) => Promise<ModPack>;
  private cardsToShow = 3;
  private isLoaded: boolean = false;

  @Watch('modpacks', {deep: true})
  public async onModpacksChange(newVal: ModpackState, oldVal: ModpackState) {
    if(JSON.stringify(newVal.installedPacks) !== JSON.stringify(oldVal.installedPacks)){
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

  public changeToList(){
    let settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = true;
    this.saveSettings(settingsCopy);
  }

  public changeToGrid(){
    let settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = false;
    this.saveSettings(settingsCopy);
  }

  public getModpack(id: number): ModPack | undefined {
      return this.modpacks?.packsCache[id];
  }

  private async mounted() {
    if (this.modpacks == null || this.modpacks.featuredPacks.length <= 0) {
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
