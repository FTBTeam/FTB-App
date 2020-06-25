<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">

    <div class="w-full flex flex-row justify-start mb-10">
      <h1 class="text-3xl mr-5 cursor-pointer" @click="goTo('modpacks')" v-bind:class="{'border-b-2': isActiveTab('modpacks')}">My Modpacks</h1>
      <h1 class="text-3xl cursor-pointer text-muted hover:text-white" @click="goTo('browseModpacks')">Browse</h1>
    </div>

    <!-- My Modpacks Stuff -->
    <div class="">
      <!--      <div class="w-1/2 self-center">-->
      <!--        <FTBSearchBar v-model="searchTerm" placeholder="Search" :doSearch="() => {}"/>-->
      <!--      </div>-->
      <div class="flex flex-col w-full">
        <div class="justify-center">
          <transition-group name="list" tag="div" class="flex pt-1 flex-wrap overflow-x-auto px-auto"
                            v-if="modpacks.installedPacks.length > 0" appear>
            <pack-card
                    v-for="modpack in packs"
                    :key="modpack.uuid"
                    :art="modpack.art"
                    :installed="true"
                    :minecraft="'1.7.10'"
                    :version="modpack.version"
                    :versions="modpack.versions"
                    :name="modpack.name"
                    :instance="modpack"
                    :instanceID="modpack.uuid">
            </pack-card>
            <pack-card :fake=true key="fake_1"></pack-card>
            <pack-card :fake=true key="fake_2"></pack-card>
            <pack-card :fake=true key="fake_3"></pack-card>
            <pack-card :fake=true key="fake_4"></pack-card>
          </transition-group>
          <div class="flex pt-1 flex-wrap overflow-x-auto justify-center" v-else>
            <!-- TODO: Make this pretty -->
            <span>You have no modpacks installed! Find one <router-link to="/browseModpacks"> here</router-link>.</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
    import {Component, Vue} from 'vue-property-decorator';
    import PackCard from '@/components/packs/PackCard.vue';
    import FTBSearchBar from '@/components/FTBSearchBar.vue';
    import {ModpackState, ModPack, Instance} from '@/modules/modpacks/types';
    import {State} from 'vuex-class';
    import {asyncForEach} from '../utils';

    @Component({
        components: {
            PackCard,
            FTBSearchBar,
        },
    })
    export default class Modpacks extends Vue {
        @State('modpacks') public modpacks: ModpackState | undefined = undefined;
        private searchTerm: string = '';
        private activeTab: string = 'overview';

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

    }
</script>
