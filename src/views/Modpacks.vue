<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-if="isLoaded">
    <!-- My Modpacks Stuff -->
    <div class="" v-if="modpacks.installedPacks.length > 0"  >
      <!--      <div class="w-1/2 self-center">-->
      <!--        <FTBSearchBar v-model="searchTerm" placeholder="Search" :doSearch="() => {}"/>-->
      <!--      </div>-->
      <div class="flex flex-col w-full">
        <div class="justify-center">
          <transition-group name="list" tag="div" class="flex pt-1 flex-wrap overflow-x-auto px-auto" appear>
            <pack-card-wrapper
                    v-for="modpack in packs"
                    :list-mode="settings.settings.listMode"
                    :key="modpack.uuid"
                    :art="modpack.art"
                    :installed="true"
                    :minecraft="'1.7.10'"
                    :version="modpack.version"
                    :description="getModpack(modpack.id) !== null ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'"
                    :tags="getModpack(modpack.id) !== null ? getModpack(modpack.id).tags : []"
                    :versions="modpack.versions"
                    :name="modpack.name"
                    :instance="modpack"
                    :instanceID="modpack.uuid">
            </pack-card-wrapper>
            <pack-card-wrapper v-if="!settings.settings.listMode" :fake=true :list-mode="settings.settings.listMode" key="fake_1"></pack-card-wrapper>
            <pack-card-wrapper v-if="!settings.settings.listMode" :fake=true :list-mode="settings.settings.listMode" key="fake_2"></pack-card-wrapper>
            <pack-card-wrapper v-if="!settings.settings.listMode" :fake=true :list-mode="settings.settings.listMode" key="fake_3"></pack-card-wrapper>
            <pack-card-wrapper v-if="!settings.settings.listMode" :fake=true  :list-mode="settings.settings.listMode" key="fake_4"></pack-card-wrapper>
          </transition-group>
        </div>
      </div>
    </div>
    <div class="flex flex-1 pt-1 flex-wrap overflow-x-auto justify-center flex-col items-center" v-else>
      <!-- TODO: Make this pretty -->
      <font-awesome-icon icon="heart-broken" style="font-size: 40vh"></font-awesome-icon>
      <h1 class="text-5xl">Oh no!</h1>
      <span>Seems you don't have any modpacks installed!</span>
      <div class="flex flex-row justify-between my-2">
        <ftb-button color="primary" class="mx-2" @click="goTo('/browseModpacks')">Browse</ftb-button>
        <ftb-button color="primary" class="mx-2" @click="goTo('/discover')">Discover</ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
    import {Component, Vue, Watch, } from 'vue-property-decorator';
    import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
    import FTBSearchBar from '@/components/FTBSearchBar.vue';
    import FTBButton from '@/components/FTBButton.vue';
    import {ModpackState, ModPack, Instance} from '@/modules/modpacks/types';
    import {State, namespace, Action, Getter} from 'vuex-class';
    import {asyncForEach} from '../utils';
    import { SettingsState } from '../modules/settings/types';

    @Component({
        components: {
            PackCardWrapper,
            FTBSearchBar,
            'ftb-button': FTBButton,
        },
    })
    export default class Modpacks extends Vue {
        @State('settings') public settings!: SettingsState;
        @State('modpacks') public modpacks!: ModpackState;
        @Getter('packsCache', {namespace: 'modpacks'}) public packsCache!: ModPack[];
        @Action('fetchModpack', {namespace: 'modpacks'}) public fetchModpack!: (id: number) => Promise<ModPack>;
        
        private searchTerm: string = '';
        private activeTab: string = 'overview';
        private isLoaded: boolean = false;

        @Watch('modpacks', {deep: true})
        public async onModpacksChange(newVal: ModpackState, oldVal:ModpackState){
          this.isLoaded = false;
          try {
           await Promise.all(this.modpacks.installedPacks.map(async (instance) =>{
              let pack = await this.fetchModpack(instance.id);
              return pack;
            }));
          this.isLoaded = true;
          } catch(err) {
            this.isLoaded = true;
          }
        }

        async mounted(){
          if(this.modpacks){
            this.isLoaded = false;
            try {
            await Promise.all(this.modpacks.installedPacks.map(async (instance) =>{
                let pack = await this.fetchModpack(instance.id);
                return pack;
            }));
            this.isLoaded = true;
            } catch(err) {
              this.isLoaded = true;
            }
          }
        }

        get packs(): Instance[] {
            return this.modpacks == null ? [] : this.searchTerm.length > 0 ? this.modpacks.installedPacks.filter((pack) => {
                return pack.name.search(new RegExp(this.searchTerm, 'gi')) !== -1;
            }) : this.modpacks.installedPacks.sort((a, b) => {
                return b.lastPlayed - a.lastPlayed;
            });
        }

        public isTabActive(tabItem: string) {
            return this.activeTab === tabItem;
        }

        public setActiveTab(tabItem: string) {
            this.activeTab = tabItem;
        }

        public goTo(page: string): void {
            // We don't care about this error!
            this.$router.push(page).catch((err) => { return; });
        }

        public isActiveTab(tab: string): boolean {
            return tab === 'home' && this.$route.path === '/' ? true : this.$route.path.startsWith(`/${tab}`);
        }

        public getModpack(id: number): ModPack | null {
          return this.packsCache[id] ? this.packsCache[id] : null;
        }

    }
</script>
