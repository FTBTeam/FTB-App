<template>
  <div class="flex flex-1 flex-col min-h-full overflow-x-hidden">
    <h1 class="m-1 mb-1 text-sm opacity-0 ">Discover</h1>
    <transition-group
      class='carousel max-w-full flex flex-row flex-1'
      v-if="discovery.discoveryQueue.length > 0"
      tag="div">
      <div
        v-for="(pack, index) in discovery.discoveryQueue"
        :key="pack.id" class="slide flex flex-col"
        style="min-width: 100%; padding: 0 20px;">
    <div class="m-4 flex-1 relative flex mt-2 mb-3">
        <div class="placeholder"></div>
        <div class="absolute top-0 left-0 bg-transparent-black w-full z-10 frosted-glass" style="height: 3rem;" v-if="!swapping">
            <p class="px-4 font-bold uppercase" style="line-height: 3rem;" v-if="getDisplayVideo(pack) !== null" >{{getDisplayVideo(pack).name}}</p>
        </div>
        <div class="absolute right-0 pl-2 flex flex-row z-10" style="bottom: 45px">
            <span v-for="(tag, i) in pack.tags.slice(0, 5)" :key="`tag-${i}`" @click="clickTag(tag.name)" class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
            <span v-if="pack.tags.length > 5" key="tag-more"  class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+ {{pack.tags.length - 5}}</span>
        </div>
        <div class="flex-1 flex flex-row justify-center items-center" style="z-index: 2;" v-if="swapping">
            <font-awesome-icon icon="spinner" spin size="4x" />
        </div>
        <div :class="`flex-1 ${swapping ? 'hidden' : 'block'}`" style="z-index: 2;" >
            <iframe v-if="index === 0 && getDisplayVideo(pack) !== null" width="100%" height="100%" @load="load" :src="iFrameURL(getDisplayVideo(pack).link)" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
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
        :authors="pack.authors"
        :versions="pack.versions"
        :description="pack.synopsis"
    >
    </pack-card-list>

      </div>
    </transition-group>
    <div v-else class='carousel max-w-full flex flex-row flex-1'>
        <div class="slide flex flex-col"
        style="min-width: 100%; padding: 0 20px; opacity:1;">
            <div class="m-4 flex-1 relative flex mt-2 mb-3">
                <div class="placeholder"></div>
                <div class="flex-1 flex flex-col justify-center items-center" style="z-index: 2;">
                    <font-awesome-icon icon="sad-tear" size="6x" style="margin-bottom: 20px"/>
                    <h2>Unfortunately we have nothing for you to discover at this time</h2>
                </div>
            </div>
        </div>
    </div>
    <div class="flex flex-row justify-between my-2" v-if="discovery.discoveryQueue.length > 0">
        <FTBButton color="primary" class="py-2 px-4 mx-2 text-center w-32" @click="previous">Previous</FTBButton>
        <FTBButton color="primary" class="py-2 px-4 mx-2 text-center w-32" @click="next">Next</FTBButton>
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
import { ModPack, ModPackLink } from '../modules/modpacks/types';

interface ModPackVideo {
    [index: number]: ModPackLink;
}

@Component({
    components: {
        PackCardList,
        FTBButton,
        Loading,
    },
})
export default class DiscoverPage extends Vue {
    @State('settings') public settingsState!: SettingsState;
    @State('discovery') public discovery: DiscoveryState | undefined = undefined;
    @Action('fetchQueue', {namespace: 'discovery'}) public fetchQueue: any;

    private index: number = 0;
    private swapping: boolean = false;
    private videos: ModPackVideo = [];


    public mounted() {
        this.fetchQueue();
    }

    public load() {
        this.swapping = false;
    }

    public clickTag(tagName: string) {
        this.$router.push({name: 'browseModpacks', params: {search: tagName}});
    }

    public previous() {
        if (!this.discovery) {
            return;
        }
        const first: ModPack | undefined = this.discovery.discoveryQueue.pop();
        if (first !== undefined) {
            this.swapping = true;
            this.discovery.discoveryQueue = [first].concat(this.discovery.discoveryQueue);
        }
    }

    public next() {
        if (!this.discovery) {
            return;
        }
        const first: ModPack | undefined = this.discovery.discoveryQueue.shift();
        if (first !== undefined) {
            this.swapping = true;
            this.discovery.discoveryQueue = this.discovery.discoveryQueue.concat(first);
        }
    }

    public getDisplayVideo(modpack: ModPack): ModPackLink | null {
        if (modpack === undefined || modpack.links === undefined) {
            return null;
        }
        if (this.videos[modpack.id]) {
            return this.videos[modpack.id];
        }
        if (modpack.links.length > 0) {
            const videos = modpack.links.filter((l) => l.type === 'video');
            if (videos.length > 0) {
                const video = videos[Math.floor(Math.random() * videos.length)];
                this.videos[modpack.id] = video;
                return video;
            }
        }
        return null;
    }

    public iFrameURL(link: string) {
        if (link.indexOf('youtube.com') !== -1) {
            const videoID = link.substring(link.indexOf('?v=') + '?v='.length, link.indexOf('&') === -1 ? link.length : link.indexOf('&'));
            return `https://www.youtube-nocookie.com/embed/${videoID}?autoplay=1&mute=1`;
        } else if (link.indexOf('twitch.tv') !== -1) {
            const videoID = link.substring(link.indexOf('videos/') + 'videos/'.length, link.length);
            return `https://player.twitch.tv/?video=${videoID}&autoplay=true&muted=true&parent=feed-the-beast.com`;
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
    opacity: 0.2;
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
