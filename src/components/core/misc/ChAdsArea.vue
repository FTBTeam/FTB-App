<template>
  <div class="ch-ads-area flex flex-col gap-4 items-center">    
    <div class="ad relative" v-for="(ad, index) in ads" :key="`${index}-ch-ad`" :class="{[ad.type]: true}">
      <div class="hover-text" v-if="ad.hover">{{ad.hover}}</div>
      
      <template v-if="ad.type === 'video'">
        <video
          width="300"
          autoplay
          muted
          loop
          style="margin: 0 auto"
          @click="ad.link && platform.get.utils.openUrl(ad.link)"
          :class="{'cursor-pointer': ad.link}"
        >
          <source :src="`${ad.asset.startsWith('http') ? ad.asset : `file://${ad.asset}`}`" :type="`video/${ad.asset.endsWith('webm') ? 'webm' : 'mp4'}`" />
        </video>
      </template>
      <div :class="{'cursor-pointer': ad.link}" v-else-if="ad.type === 'image'" @click="ad.link && platform.get.utils.openUrl(ad.link)">
        <img :src="ad.asset" alt="Advertisement for CreeperHost Minecraft server provider" />
      </div>
      <div @click="ad.link && platform.get.utils.openUrl(ad.link)" :class="{'cursor-pointer': ad.link}" v-else-if="ad.type === 'text'">
        <p class="text-sm" v-if="!ad.markdown">{{ getText(ad) }}</p>
        <div class="text-sm wysiwyg" v-else v-html="getText(ad)" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import {Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {Ad} from '@/core/@types/external/metaApi.types';
import {parseMarkdown} from '@/utils';

@Component
export default class ChAdsArea extends Vue {
  platform = platform;
  
  @Getter('ads', ns('v2/ads')) ads!: Ad[];

  getText(ad: Ad) {
    if (ad.type !== 'text') return '';
    
    if (ad.markdown) {
      return parseMarkdown(ad.value)
    }
    
    return ad.value;
  }
  
  get isElectron() {
    return platform.isElectron();
  }
  
  get isDevEnv() {
    return false; // process.env.NODE_ENV !== 'production';
  }
}
</script>

<style lang="scss" scoped>
.ad {
  &:hover {
    .hover-text {
      visibility: visible;
      transform: translateY(calc(-100% - .5rem));
      opacity: 1;
    }
  }
}

.hover-text {
  position: absolute;
  top: 0;
  right: 0;
  transform: translateY(calc(-100% - .5rem));
  max-width: 300px;
  background-color: var(--color-navbar);
  border-radius: 3px;
  font-size: 14px;
  padding: .3rem .5rem;
  
  visibility: hidden;
  opacity: 0;
  transition: visibility .25s ease-in-out, opacity .25s ease-in-out, transform .25s ease-in-out;
}
</style>