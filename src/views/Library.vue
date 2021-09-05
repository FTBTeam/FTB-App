<template>
  <div class="mod-packs h-full" v-if="isLoaded">
    <!-- My Modpacks Stuff -->
    <div class="packs px-6 py-4" v-if="modpacks.installedPacks.length > 0">
      <div class="w-1/2 self-center">
        <FTBSearchBar v-model="searchTerm" placeholder="Search" class="mb-4" />
      </div>
      <div class="pack-card-list grid">
        <pack-card-wrapper
          v-for="modpack in packs"
          :list-mode="false"
          :key="modpack.uuid"
          :art="modpack.art"
          :installed="true"
          :minecraft="'1.7.10'"
          :version="modpack.version"
          :description="getModpack(modpack.id) !== null ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'"
          :tags="getModpack(modpack.id) !== null ? getModpack(modpack.id).tags : []"
          :versions="modpack.versions"
          :name="modpack.name"
          :authors="modpack.authors"
          :instance="modpack"
          :instanceID="modpack.uuid"
          :kind="modpack.kind"
          :preLaunch="preLaunch"
        >
        </pack-card-wrapper>
      </div>
    </div>
    <div class="flex flex-1 flex-wrap justify-center flex-col items-center no-packs" v-else>
      <div class="message flex flex-1 flex-wrap items-center flex-col mt-32">
        <font-awesome-icon icon="heart-broken" size="6x" />
        <h1 class="text-5xl">Oh no!</h1>
        <span class="mb-4 w-3/4 text-center">
          Would you look at that! Looks like you've got no modpacks installed yet... If you know what you want, click
          Browse and search through our collection and all of CurseForge modpacks, otherwise, use Discover we've got
          some great recommended packs.</span
        >
        <div class="flex flex-row justify-between my-2">
          <router-link to="/browseModpacks">
            <ftb-button color="primary" class="py-2 px-10 mx-2">Browse</ftb-button>
          </router-link>
          <router-link to="/discover">
            <ftb-button color="primary" class="py-2 px-6 mx-2">Discover</ftb-button>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import FTBSearchBar from '@/components/FTBSearchBar.vue';
import { Instance, ModPack, ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';

@Component({
  components: {
    PackCardWrapper,
    FTBSearchBar,
  },
})
export default class Library extends Vue {
  @State('settings') public settings!: SettingsState;
  @State('modpacks') public modpacks!: ModpackState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Getter('packsCache', { namespace: 'modpacks' }) public packsCache!: ModPack[];
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: (id: number) => Promise<ModPack>;
  @Action('sendMessage') public sendMessage!: any;

  private searchTerm: string = '';
  private isLoaded: boolean = false;
  isGrid: boolean = false;

  @Watch('modpacks', { deep: true })
  public async onModpacksChange(newVal: ModpackState, oldVal: ModpackState) {
    if (JSON.stringify(newVal.installedPacks) !== JSON.stringify(oldVal.installedPacks)) {
      this.isLoaded = false;
      try {
        await Promise.all(
          this.modpacks.installedPacks.map(async instance => {
            const pack = await this.fetchModpack(instance.id);
            return pack;
          }),
        );
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  /**
   * @MichaelHillcox I didn't comment this out, just saying, no clue what this does :+1:
   */
  public preLaunch(instance: Instance) {
    //   let serverID = "283861";
    // let newArgs = instance.jvmArgs;
    // if(newArgs.indexOf("-Dmt.server") !== 1){
    //   let args = newArgs.split(" ");
    //   args.splice(args.findIndex(value => value.indexOf("-Dmt.server") !== -1), 1);
    //   newArgs = args.join(" ");
    // }
    // if(newArgs[newArgs.length - 1] === " " || newArgs.length === 0){
    //   newArgs += "-Dmt.server=" + serverID;
    // } else {
    //   newArgs += " -Dmt.server=" + serverID;
    // }
    // return new Promise((res, rej) => {
    //   this.sendMessage({
    //       payload: {type: 'instanceConfigure', uuid: instance.uuid, instanceInfo: {jvmargs: newArgs}}, callback: (data: any) => {
    //           res();
    //       },
    //   });
    // })
  }

  public async mounted() {
    if (this.modpacks) {
      this.isLoaded = false;
      try {
        await Promise.all(
          this.modpacks.installedPacks.map(async instance => {
            const pack = await this.fetchModpack(instance.id);
            return pack;
          }),
        );
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  get packs(): Instance[] {
    return this.modpacks == null
      ? []
      : this.searchTerm.length > 0
      ? this.modpacks.installedPacks.filter(pack => {
          return pack.name.search(new RegExp(this.searchTerm, 'gi')) !== -1;
        })
      : this.modpacks.installedPacks.sort((a, b) => {
          return b.lastPlayed - a.lastPlayed;
        });
  }

  public getModpack(id: number): ModPack | null {
    return this.packsCache[id] ? this.packsCache[id] : null;
  }
}
</script>

<style lang="scss" scoped>
.pack-card-list {
  &.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, 148px);
    gap: 1rem;
  }
}

.no-packs {
  position: relative;
  height: 100%;

  &::before {
    content: '';
    top: 0;
    left: 0;
    position: absolute;
    width: 100%;
    height: 100%;

    background: url('../assets/images/no-pack-bg.webp') center center no-repeat;
    background-size: auto 100%;
    z-index: -1;
    opacity: 0.3;
  }
}
</style>
