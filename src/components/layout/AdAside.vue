<script lang="ts" setup>
import appPlatform from '@platform';
import {createLogger} from '@/core/logger';
import {onMounted, useTemplateRef, ref, computed, nextTick} from 'vue';
import { useAttachDomEvent } from '@/composables';
import { useAds } from '@/composables/useAds.ts';
import OwAdViewWrapper from '@/components/layout/OwAdViewWrapper.vue';

const logger = createLogger("AdAside.vue");
const adsHook = useAds();

const { hideAds = false } = defineProps<{
  hideAds: boolean;
}>()

const ads = ref<Record<string, any>>({});
const disableSmallerAd = ref(false);
const highImpactAdLoaded = ref(false);

const adOneRef = useTemplateRef('adRef');
const adTwoRef = useTemplateRef('adRefSecond');

const isElectron = appPlatform.isElectron;
const isSmallDisplay = computed(() => (window as any)?.ftbFlags?.smallMonitor);

useAttachDomEvent<UIEvent>('resize', onResize);

onMounted(() => {
  logger.info('Loaded ad sidebar widget');
  onResize()
  
  if (isElectron) {
    return;
  }

  setTimeout(() => {
    loadAds("ad-1", adOneRef.value, {size: [{ width: 400, height: 600 }, { width: 400, height: 300 }], enableHighImpact: true});
    if (!(window as any)?.ftbFlags?.smallMonitor) {
      loadAds("ad-2", adTwoRef.value, {
        size: {
          width: 300,
          height: 250
        }
      });
    }
  }, 1500);
})

function onResize() {
  if (window.outerHeight < 890 && !disableSmallerAd.value) {
    disableSmallerAd.value = true;
  }

  if (window.outerHeight > 890 && disableSmallerAd.value) {
    disableSmallerAd.value = false;
  }
}

async function loadAds(id: string, elm: any, options?: any) {
  logger.info(`[AD: ${id}] Loading advert system for ${id}`);

  if (typeof OwAd === 'undefined' || !OwAd) {
    logger.info(`[AD: ${id}] No advert object available`);
    return;
  }

  logger.info(`[AD: ${id}] Created advert object`);

  if (ads.value[id]) {
    ads.value[id].shutdown();
  }

  ads.value[id] = new OwAd(elm, options);

  const win = window as any;
  if (!win.ads) {
    win.ads = {};
  }

  win.ads[id] = ads.value[id];
  ads.value[id].addEventListener('error', (error: any) => {
    logger.info(`[AD: ${id}] Failed to load ad`, error);
  });
  ads.value[id].addEventListener('player_loaded', () => {
    logger.info(`[AD: ${id}] Player loaded`);
  });
  ads.value[id].addEventListener('display_ad_loaded', () => {
    logger.info(`[AD: ${id}] Display ad loaded and ready`);
  });
  ads.value[id].addEventListener('play', () => {
    logger.info(`[AD: ${id}] Ad ready and loaded`);
  });
  ads.value[id].addEventListener('complete', () => {
    logger.info(`[AD: ${id}] Video ad finished playing`);
  });
  
  if (options?.enableHighImpact) {
    ads.value[id].addEventListener('high-impact-ad-loaded', () => {
      logger.info(`[AD: ${id}] High impact ad loaded`);
      highImpactAdStateChanged('loaded');
    });
    
    ads.value[id].addEventListener('high-impact-ad-removed', () => {
      logger.info(`[AD: ${id}] High impact ad removed`);
      highImpactAdStateChanged('removed');
    });
  }
}

function highImpactAdStateChanged(state: 'loaded' | 'removed') {
  highImpactAdLoaded.value = state === 'loaded';
  if (state === 'loaded') {
    if (ads.value["ad-2"]) {
      logger.info('High impact ad loaded, shutting down smaller ad');
      ads.value["ad-2"].shutdown();
    }
  } else {
    logger.info('High impact ad removed, reloading smaller ad');
    nextTick(() => {
      if (isSmallDisplay.value) {
        return;
      }
      
      // Reload the ad when high impact ad is removed
      loadAds("ad-2", adTwoRef.value, {
        size: {
          width: 300,
          height: 250
        }
      });
    })
  }
}
</script>

<template>
  <div class="ad-aside" :class="{ 'electron': isElectron }" v-if="adsHook.adsEnabled.value && !hideAds">
    <div class="ad-container ads overwolf" v-if="!isElectron" key="adside-ad-type-ow">
      <div class="ad-holder small" v-if="!isSmallDisplay && !disableSmallerAd && !highImpactAdLoaded">
        <div
          id="ow-ad-second"
          ref="adRefSecond"
          style="width: 300px; height: 250px;"
        />
      </div>

      <div class="ad-holder" :style="{
        flex: highImpactAdLoaded ? '1' : undefined,
      }">
        <div
          id="ow-ad"
          ref="adRef"
          :style="{
            width: highImpactAdLoaded ? '440px' : '400px',
            height: highImpactAdLoaded ? '100%' : '600px',
            minHeight: highImpactAdLoaded ? '670px' : '600px'
          }"
        />
      </div>
    </div>

    <div class="ad-container ads" v-else key="adside-ad-type">
      <OwAdViewWrapper width="300px" height="250px" v-if="!disableSmallerAd && !highImpactAdLoaded" />
      <OwAdViewWrapper :width="highImpactAdLoaded ? '440px' : '400px'" :height="highImpactAdLoaded ? undefined : '600px'" enableHighImpact @highImpactAdStateChanged="highImpactAdStateChanged" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ad-aside {
  position: relative;
  background-color: black;
  min-width: 440px;
  height: 100%;

  border-left: 2px solid rgba(black, 0.1);

  .ad-container {
    height: 100%;
    display: flex;
    align-items: center;
    flex-direction: column;
    justify-content: flex-end;
    gap: 1rem;
  } 
  
  .ad-container.overwolf {
    position: relative;

    .ad-holder {
      position: relative;
    }
  }
  
  .ads {
    display: flex;
    align-items: center;
    flex-direction: column;
    position: relative;
    z-index: 1;

    .ad-holder {
      position: relative;

      &.small {
        margin-bottom: .5rem;
      }
    }
  }
}
</style>
