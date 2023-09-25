<template>
  <div class="home page-spacing">    
    <template v-if="recentInstances.length">
      <h2 class="text-lg font-bold text-white mb-4">Jump back in where you left off</h2>
      
      <div class="recently-played pack-card-grid mb-5">
        <pack-card2 class="pack-card" v-for="instance in recentInstances" :key="instance.uuid" :instance="instance" />
      </div>
    </template>
    
    <div class="featured-packs">
      <h2 class="text-lg font-bold text-white mb-4">Featured packs</h2>
      <pack-preview v-for="packId in featuredPacksIds" :key="packId" :packId="packId" provider="modpacksch" />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import FtbButton from '@/components/atoms/input/FTBButton.vue';
import Loading2 from '@/components/atoms/Loading2.vue';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import PackPreview from '@/components/core/modpack/PackPreview.vue';
import PackCard2 from '@/components/core/modpack/PackCard2.vue';
import {alertController} from '@/core/controllers/alertController';
import {dialogsController} from '@/core/controllers/dialogsController';
import {Dialog} from '@/core/state/misc/dialogsState';

@Component({
  components: {
    PackCard2,
    PackPreview,
    Loading2,
    FtbButton,
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
  .pack-card {
    @media (max-width: 1288px) {
      &:nth-child(5) {
        display: none;
      }
    }
    
    @media (max-width: 1458px) {
        &:nth-child(6) {
          display: none;
        }
    }
  }
}
</style>
