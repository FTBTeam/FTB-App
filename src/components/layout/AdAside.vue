<script lang="ts" setup>
import appPlatform from '@platform';
import {createLogger} from '@/core/logger';
import { onMounted, useTemplateRef, ref, computed } from 'vue';
import { useAttachDomEvent } from '@/composables';
import { constants } from '@/core/constants.ts';
import { useAds } from '@/composables/useAds.ts';
import OwAdViewWrapper from '@/components/ui/OwAdViewWrapper.vue';

const logger = createLogger("AdAside.vue");
const adsHook = useAds();

const { hideAds = false } = defineProps<{
  hideAds: boolean;
}>()

const ads = ref<Record<string, any>>({});
const showAdOnePlaceholder = ref(true);
const showAdTwoPlaceholder = ref(true);
const disableSmallerAd = ref(false);

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
    loadAds("ad-1", (value) => showAdOnePlaceholder.value = value, adOneRef.value?.adRef, {size: [{ width: 400, height: 600 }, { width: 400, height: 300 }]});
    if (!(window as any)?.ftbFlags?.smallMonitor) {
      loadAds("ad-2", (value) => showAdTwoPlaceholder.value = value, adTwoRef.value?.adRefSecond, {
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

const isDevEnv = constants.isDevelopment;

async function loadAds(id: string, emitPlaceholderUpdate: (state: boolean) => void, elm: any, options?: any) {
  emitPlaceholderUpdate(true);

  if (isDevEnv) {
    return;
  }

  logger.info(`[AD: ${id}] Loading advert system for ${id}`);

  if (typeof OwAd === 'undefined' || !OwAd) {
    emitPlaceholderUpdate(true);
    logger.info(`[AD: ${id}] No advert object available`);
    return;
  }

  logger.info(`[AD: ${id}] Created advert object`);

  ads.value[id] = new OwAd(elm, options);

  const win = window as any;
  if (!win.ads) {
    win.ads = {};
  }

  win.ads[id] = ads.value[id];
  ads.value[id].addEventListener('error', (error: any) => {
    emitPlaceholderUpdate(true);
    logger.info(`[AD: ${id}] Failed to load ad`, error);
  });
  ads.value[id].addEventListener('player_loaded', () => {
    logger.info(`[AD: ${id}] Player loaded`);
  });
  ads.value[id].addEventListener('display_ad_loaded', () => {
    emitPlaceholderUpdate(false);
    logger.info(`[AD: ${id}] Display ad loaded and ready`);
  });
  ads.value[id].addEventListener('play', () => {
    emitPlaceholderUpdate(false);
    logger.info(`[AD: ${id}] Ad ready and loaded`);
  });
  ads.value[id].addEventListener('complete', () => {
    logger.info(`[AD: ${id}] Video ad finished playing`);
  });
}
</script>

<template>
  <div class="ad-aside" :class="{ 'electron': isElectron }">
    <template v-if="!hideAds">
      <div class="ad-container ads overwolf" v-if="!isElectron" key="adside-ad-type-ow">
        <div class="ad-holder small" v-if="!isSmallDisplay && !disableSmallerAd">
          <div
            v-if="!isElectron"
            v-show="adsHook.adsEnabled ?? true"
            id="ow-ad-second"
            ref="adRefSecond"
            style="max-width: 300px; max-height: 250px;"
          />
          <div class='place-holder small' v-if="!isElectron && showAdTwoPlaceholder" />
        </div>
  
        <div class="ad-holder">
          <div
            v-if="!isElectron"
            v-show="adsHook.adsEnabled ?? true"
            id="ow-ad"
            ref="adRef"
            style="max-width: 400px; max-height: 600px;"
          />
  
          <div class='place-holder' v-if="!isElectron && showAdOnePlaceholder">
            <img src="@/assets/images/ftb-logo.svg" class="mb-8" width="70" alt="FTB Logo">
            <p class="mb-1">Thank you for supporting FTB ❤️</p>
            <p>Ads support FTB & CurseForge Authors!</p>
          </div>
        </div>
      </div>
  
      <div class="ad-container ads" v-else key="adside-ad-type">
        <div class="ad-holder small" v-if="!disableSmallerAd">
          <div style="width: 300px; height: 250px; background: transparent;">
            <OwAdViewWrapper />
          </div>
        </div>
        
        <div class="ad-holder">
          <div style="width: 400px; height: 600px; background: transparent;">
            <OwAdViewWrapper />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.ad-aside {
  position: relative;
  background-color: black;
  min-width: 400px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;

  border-left: 2px solid rgba(black, 0.1);
  padding-bottom: .4rem;

  .ad-container.overwolf {
    position: relative;
    min-width: 400px;
    height: 100%;

    .ad-holder {
      position: relative;
      width: 400px;
      height: 600px;

      &.small {
        width: 300px;
        height: 250px;
      }
    }
  }
  
  .ads {
    display: flex;
    align-items: center;
    flex-direction: column;
    position: relative;
    z-index: 1;

    &.ad-container {
    }

    .ad-holder {
      position: relative;

      &.small {
        margin-bottom: .5rem;
      }

      .place-holder {
        position: absolute;
        width: 400px;
        height: 600px;
        background: black;
        border-radius: 5px;
        top: 0;
        left: 0;

        display: flex;
        align-items: center;
        justify-content: center;

        flex-direction: column;
        padding: 3rem;
        text-align: center;
        font-weight: bold;
        z-index: -1;

        span {
          display: block;
          margin-bottom: 1rem;
        }

        .logo {
          max-width: 60px;
          margin-bottom: 1rem;
        }

        &.small {
          height: 250px;
          left: -50px;
        }
      }
    }
  }
}
</style>
