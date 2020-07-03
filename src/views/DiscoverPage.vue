<template>
  <div class="flex flex-1 flex-col min-h-full overflow-x-hidden">
    <h1 class="m-4 mb-1 text-2xl">Discover</h1>
    <transition-group
      class='carousel max-w-full flex flex-row flex-1'
      tag="div">
      <div 
        v-for="pack in discovery.discoveryQueue"
        :key="pack.id" class="slide flex flex-col" 
        style="min-width: 100%; padding: 0 20px;">
    <div class="m-4 flex-1 relative flex mt-2 mb-3">
        <div class="placeholder"></div>
        <div class="absolute top-0 left-0 bg-transparent-black w-full z-10 frosted-glass" style="height: 3rem;" v-if="!swapping">
            <p class="pt-2 p-4 pb-1 font-bold uppercase" style="line-height: 3rem;">Direwolf20 plays FTB Revelations</p>
        </div>
        <div class="absolute right-0 pl-2 flex flex-row z-10" style="bottom: 45px">
            <span v-for="(tag, i) in pack.tags.slice(0, 5)" :key="`tag-${i}`" @click="clickTag(tag.name)" class="cursor-pointer rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
            <span v-if="pack.tags.length > 5" key="tag-more"  class="rounded mx-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+ {{pack.tags.length - 5}}</span>
        </div>
        <div class="flex-1 flex flex-row justify-center items-center" style="z-index: 2;" v-if="swapping">
            <font-awesome-icon icon="spinner" spin size="4x" />
        </div>
        <div :class="`flex-1 ${swapping ? 'hidden' : 'block'}`" style="z-index: 2;" >
            <iframe width="100%" height="100%" @load="load"  src="https://www.youtube-nocookie.com/embed/MxbYy4ROFVc?autoplay=1&mute=1" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
        </div>
    </div>
    <pack-card-list
        class="mx-auto"
        size="1"
        :packID="pack.id"
        :art="pack.art.length > 0 ? pack.art.filter((art) => art.type === 'square')[0].url : ''"
        :installed="false"
        :minecraft="'1.7.10'"
        :version="pack.versions.length > 0 ? pack.versions[0].name : 'unknown'"
        :versionID="pack.versions[0].id"
        :name="pack.name"
        :versions="pack.versions"
        :description="pack.synopsis"
    >
    </pack-card-list>

      </div>
    </transition-group>
    <div class="flex flex-row justify-between my-2">
        <FTBButton color="primary" class="mx-2 text-center w-32" @click="previous">Previous</FTBButton>
        <FTBButton color="primary" class="mx-2 text-center w-32" @click="next">Next</FTBButton>
    </div>
  </div>
</template>

<script lang="ts">  
import {Component, Vue} from 'vue-property-decorator';
import PackCardList from '@/components/packs/PackCardList.vue';
import FTBButton from '@/components/FTBButton.vue';
import Loading from '@/components/Loading.vue';
import {State, Action, Getter} from 'vuex-class';
import {NewsState, NewsItem} from '@/modules/news/types';
import { SettingsState } from '../modules/settings/types';
import { DiscoveryState } from '../modules/discovery/types';
import { ModPack } from '../modules/modpacks/types';

@Component({
    components: {
        PackCardList,
        FTBButton,
        Loading
    }
})
export default class DiscoverPage extends Vue {
    @State('settings') public settingsState!: SettingsState;
    @State('discovery') public discovery: DiscoveryState | undefined = undefined;
    @Action('fetchQueue', {namespace: 'discovery'}) public fetchQueue: any;

    private index: number = 0;
    private swapping: boolean = false;


    mounted(){
        this.fetchQueue();
    }

    load(){
        this.swapping = false;
    }

    public clickTag(tagName: string){
        this.$router.push({name: 'browseModpacks', params: {search: tagName}})
    }

    previous(){
        if(!this.discovery){
            return;
        }
        const first: ModPack | undefined = this.discovery.discoveryQueue.pop()
        if(first !== undefined){
            this.swapping = true;
            this.discovery.discoveryQueue = [first].concat(this.discovery.discoveryQueue)
        }
    }

    next(){
        if(!this.discovery){
            return;
        }
        const first: ModPack | undefined = this.discovery.discoveryQueue.shift()
        if(first !== undefined){
            this.swapping = true;
            this.discovery.discoveryQueue = this.discovery.discoveryQueue.concat(first)
        }
    }
}
</script>

<style scoped lang="scss">
.slide {
    transition: transform 0.3s ease-in-out;
}
.slide:first-of-type {
}
.slide:last-of-type {
  opacity: 0;
}
  .placeholder{
    background-image: url("../assets/backgrounds/background1.png");
    background-size: cover;
    background-position: center;
    width: 100%;
    height: 100%;
    z-index: 0;
    pointer-events: none;
    position: absolute;
    text-align: center;
    filter: saturate(0%);
  }
  .frosted-glass {
      backdrop-filter: blur(8px);
      background: linear-gradient(to bottom, rgba(36, 40, 47, 0) 0%, rgba(43, 57, 66, 0.2) calc( 100% - 2px), rgba(193, 202, 207, 0.1) calc( 100% - 1px), rgba(29, 29, 29, 0.3) 100%);
    //   -webkit-mask-image: linear-gradient(180deg, rgba(0,0,0,1) 50%, rgba(0,0,0,0) 100%);
  }
</style>
