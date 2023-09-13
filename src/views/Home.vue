<template>
  <div class="home page-spacing">
    <h2 class="text-lg font-bold text-white mb-4">Jump back in where you left off</h2>
    
    <div class="recently-played mb-5">
      <pack-card2 class="pack-card" v-for="instance in recentInstances" :key="instance.uuid" :instance="instance" />
    </div>
    
    <div class="featured-packs">
      <h2 class="text-lg font-bold text-white mb-4">Featured packs</h2>
      <pack-preview v-for="packId in featuredPacksIds" :key="packId" :packId="packId" provider="modpacksch" />
    </div>
  </div>
<!--  <div class="px-6 py-4" v-if="isLoaded">-->
<!--    <div class="flex flex-col" v-if="recentlyPlayed.length >= 1" key="recentlyPlayed">-->
<!--      <h1 class="text-2xl mb-2">Recently Played</h1>-->
<!--      <div class="mod-pack-grid mb-4">-->
<!--        <pack-card-->
<!--          v-for="modpack in recentlyPlayed"-->
<!--          class="pack-card-item"-->
<!--          :key="modpack.uuid"-->
<!--          :versions="modpack.versions"-->
<!--          :art="modpack.art"-->
<!--          :installed="true"-->
<!--          :version="modpack.version"-->
<!--          :name="modpack.name"-->
<!--          :authors="modpack.authors"-->
<!--          :instance="modpack"-->
<!--          :instanceID="modpack.uuid"-->
<!--          :description="-->
<!--            getModpack(modpack.id) !== undefined ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'-->
<!--          "-->
<!--          :tags="getModpack(modpack.id) !== undefined ? getModpack(modpack.id).tags : []"-->
<!--          :kind="modpack.kind"-->
<!--        />-->
<!--      </div>-->
<!--    </div>-->
<!--    <div class="flex flex-col" key="featuredPacks">-->
<!--      <h1 class="text-2xl mb-4 mt-2">Featured Modpacks</h1>-->
<!--      <div class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch">-->
<!--        <pack-card-list-->
<!--          v-for="modpack in modpacks.featuredPacks.slice(0, cardsToShow)"-->
<!--          :key="modpack.id"-->
<!--          :packID="modpack.id"-->
<!--          :versions="modpack.versions"-->
<!--          :art="modpack.art"-->
<!--          :installed="false"-->
<!--          :minecraft="'1.7.10'"-->
<!--          :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"-->
<!--          :versionID="modpack.versions[0].id"-->
<!--          :name="modpack.name"-->
<!--          :authors="modpack.authors"-->
<!--          :description="modpack.synopsis"-->
<!--          :tags="modpack.tags"-->
<!--          :kind="modpack.kind"-->
<!--          >{{ modpack.id }}</pack-card-list-->
<!--        >-->
<!--      </div>-->
<!--    </div>-->
<!--  </div>-->
<!--  <div class="loading-screen" v-else>-->
<!--    <Loading2 />-->
<!--    <div class="packs-to-load" v-if="modpacks.packsLoaded">-->
<!--      <span>{{ modpacks.packsLoaded }} / {{ modpacks.packsToLoad }}</span> Modpacks Loaded-->
<!--    </div>-->
<!--  </div>-->
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import PackCardWrapper from '@/components/organisms/packs/PackCardWrapper.vue';
import PackCard from '@/components/organisms/packs/PackCard.vue';
import PackCardList from '@/components/organisms/packs/PackCardList.vue';
import ServerCard from '@/components/organisms/ServerCard.vue';
import {Action, Getter} from 'vuex-class';
import FtbButton from '@/components/atoms/input/FTBButton.vue';
import Loading from '@/components/atoms/Loading.vue';
import Loading2 from '@/components/atoms/Loading2.vue';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import PackPreview from '@/components/core/modpack/PackPreview.vue';
import PackCard2 from '@/components/core/modpack/PackCard2.vue';

@Component({
  components: {
    PackCard2,
    PackPreview,
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
  @Getter('instances', ns("v2/instances")) instances!: SugaredInstanceJson[];
  
  @Getter("featuredPacks", ns("v2/modpacks")) featuredPacksIds!: number[];
  @Action("getFeaturedPacks", ns("v2/modpacks")) getFeaturedPacks!: () => Promise<number[]>;
  
  loadingFeatured = false;
  
  mounted() {
    this.loadFeaturedPacks();
  }
  
  async loadFeaturedPacks() {
    this.loadingFeatured = true;
    await this.getFeaturedPacks();
    this.loadingFeatured = false;
  }
  
  get recentInstances() {
    return this.instances
      .sort((a, b) => {
        return b.lastPlayed - a.lastPlayed;
      })
      .slice(0, 6)
  }
}
</script>

<style lang="scss" scoped>
.recently-played {
  display: flex;
  overflow-x: auto;
  gap: 1rem;
  padding-bottom: 1rem;
  
  .pack-card {
    min-width: 170px;
    width: 170px;
  }
}
</style>
