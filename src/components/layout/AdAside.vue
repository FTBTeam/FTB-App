<template>
  <div class="ad-aside" :class="{ 'electron': isElectron }">
    <template v-if="!hideAds">
      <div class="ad-container ads overwolf" v-if="!isElectron" key="adside-ad-type">
        <div class="ad-holder small" v-if="!isSmallDisplay">
          <div
            v-if="!isElectron"
            v-show="advertsEnabled ?? true"
            id="ow-ad-second"
            ref="adRefSecond"
            style="max-width: 300px; max-height: 250px;"
          />
          <div class='place-holder small' v-if="!isElectron && showAdTwoPlaceholder" />
        </div>
  
        <div class="ad-holder">
          <div
            v-if="!isElectron"
            v-show="advertsEnabled ?? true"
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
        <div class="ad-holder small">
          <div style="width: 300px; height: 250px; background: transparent;">
            <owadview />
          </div>
        </div>
        
        <div class="ad-holder">
          <div style="width: 400px; height: 600px; background: transparent;">
            <owadview />
          </div>
        </div>
      </div>
      
      <!-- TODO: Workout what I wanna do here -->
      <div class="ad-container ads-electron electron" v-if="false" key="adside-ad-type">
        <ch-ads-area />
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import Component from 'vue-class-component';
import {Getter, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import {JavaFetch} from '@/core/javaFetch';
import ChAdsArea from '@/components/core/misc/ChAdsArea.vue';
import {createLogger} from '@/core/logger';
import {ns} from '@/core/state/appState';
import {MineTogetherAccount} from '@/core/@types/javaApi';
import {adsEnabled} from '@/utils';
import {Prop} from 'vue-property-decorator';

@Component({
  components: {ChAdsArea}
})
export default class AdAside extends Vue {
  @State('settings') public settings!: SettingsState;
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;
  @Getter("getDebugDisabledAdAside", {namespace: 'core'}) private debugDisabledAdAside!: boolean
  
  @Prop({default: false}) public hideAds!: boolean;
  
  private logger = createLogger("AdAside.vue");

  ads: Record<string, any> = {};
  platform = platform;
  showAdOnePlaceholder = true;
  showAdTwoPlaceholder = true;

  get isElectron() {
    return platform.isElectron();
  }

  get isSmallDisplay() {
    return (window as any)?.ftbFlags?.smallMonitor;
  }
  
  async mounted() {
    this.logger.info('Loaded ad sidebar widget');
    
    // Kinda dirty hack for this file
    if (this.isElectron) {
      return;
    }

    // TODO: Figure this out
    setTimeout(() => {
      this.loadAds("ad-1", (value) => this.showAdOnePlaceholder = value, this.$refs.adRef, {size: [{ width: 400, height: 600 }, { width: 400, height: 300 }]});
      if (!(window as any)?.ftbFlags?.smallMonitor) {
        this.loadAds("ad-2", (value) => this.showAdTwoPlaceholder = value, this.$refs.adRefSecond, {
          size: {
            width: 300,
            height: 250
          }
        });
      }
    }, 1500);
  }

  async loadAds(id: string, emitPlaceholderUpdate: (state: boolean) => void, elm: any, options?: any) {
    emitPlaceholderUpdate(true);

    if (this.isDevEnv) {
      return;
    }

    this.logger.info(`[AD: ${id}] Loading advert system for ${id}`);

    if (typeof OwAd === 'undefined' || !OwAd) {
      emitPlaceholderUpdate(true);
      this.logger.info(`[AD: ${id}] No advert object available`);
      return;
    }

    this.logger.info(`[AD: ${id}] Created advert object`);

    this.ads[id] = new OwAd(elm, options);
    
    const win = window as any;
    if (!win.ads) {
      win.ads = {};
    } 
    
    win.ads[id] = this.ads[id];
    this.ads[id].addEventListener('error', (error: any) => {
      emitPlaceholderUpdate(true);
      this.logger.info(`[AD: ${id}] Failed to load ad`);
    });
    this.ads[id].addEventListener('player_loaded', () => {
      this.logger.info(`[AD: ${id}] Player loaded`);
    });
    this.ads[id].addEventListener('display_ad_loaded', () => {
      emitPlaceholderUpdate(false);
      this.logger.info(`[AD: ${id}] Display ad loaded and ready`);
      JavaFetch.modpacksCh("analytics/ads/static")
        .execute()
        .catch(e => this.logger.error("Failed to send analytics", e))
    });
    this.ads[id].addEventListener('play', () => {
      emitPlaceholderUpdate(false);
      this.logger.info(`[AD: ${id}] Ad ready and loaded`);
    });
    this.ads[id].addEventListener('complete', () => {
      this.logger.info(`[AD: ${id}] Video ad finished playing`);
    });
    this.ads[id].addEventListener('impression', () => {
      JavaFetch.modpacksCh("analytics/ads/video")
        .execute()
        .catch(e => this.logger.error("Failed to send analytics", e))
    });
  }
  
  get advertsEnabled(): boolean {
    return adsEnabled(this.settings.settings, this.getMtAccount, this.debugDisabledAdAside);
  }

  get isDevEnv() {
    return process.env.NODE_ENV !== 'production';
  }
}
</script>

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
