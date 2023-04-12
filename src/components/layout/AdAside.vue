<template>
  <div class="ad-aside" :class="{ 'is-dev': isDev }">
    <!--    <div class="ftb-ad-frame" v-if="!isDevEnv">-->
    <!--      <ins :data-revive-zoneid="adZone" data-revive-target="_blank" data-revive-id="3c373f2ff71422c476e109f9079cb399"></ins>-->
    <!--    </div>-->

    <div class="ad-container ads" v-if="!isElectron" key="adside-ad-type">
<!--      <div class="ad-holder small mb-4">-->
<!--        <div-->
<!--          v-if="!isElectron"-->
<!--          v-show="advertsEnabled ?? true"-->
<!--          id="ow-ad-second"-->
<!--          ref="adRefSecond"-->
<!--          style="max-width: 300px; max-height: 250px;"-->
<!--        />-->
<!--        <div class='place-holder small' v-if="!isElectron && showAdTwoPlaceholder"><img class="logo" src="@/assets/images/ftb-logo.svg" alt="FTB Logo" />Thank you for using the FTB App</div>-->
<!--      </div>-->

      <div class="ad-holder">
        <div
          v-if="!isElectron"
          v-show="advertsEnabled ?? true"
          id="ow-ad"
          ref="adRef"
          style="max-width: 400px; max-height: 300px;"
        />

        <div class='place-holder' v-if="!isElectron && showAdOnePlaceholder"><span>❤️</span>These ads support FTB & CurseForge Mod Authors</div>
      </div>
    </div>

    <div class="ad-container ads-electron electron" v-else key="adside-ad-type">
      <video
        @click="platform.get.utils.openUrl('https://go.ftb.team/creeperhost')"
        class="cursor-pointer"
        width="300"
        height="250"
        autoplay
        muted
        loop
        style="margin: 0 auto"
        v-if="isElectron && !isDevEnv && advertsEnabled"
      >
        <source src="https://dist.modpacks.ch/windows_desktop_src_assets_CH_AD.mp4" type="video/mp4" />
      </video>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import Component from 'vue-class-component';
import { State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';
import { Prop } from 'vue-property-decorator';
import { getLogger } from '@/utils';

@Component
export default class AdAside extends Vue {
  @State('settings') public settings!: SettingsState;
  @State('auth') public auth!: AuthState;
  @Prop({ default: false }) public isDev!: boolean;

  private logger = getLogger('ad-aside-vue');

  ads: Record<string, any> = {};
  platform = platform;
  showAdOnePlaceholder = true;
  showAdTwoPlaceholder = true;

  random = AdAside.mkRandom();

  get isElectron() {
    return platform.isElectron();
  }

  async mounted() {
    this.logger.info('Loaded ad sidebar widget');

    // I've lost brain cells from this. Please just fucking work 
    // - Covers
    // if (!this.isDevEnv) {
    //   const helpMe = document.createElement("script");
    //   helpMe.src = "https://adserver.ftb.team/www/delivery/asyncjs.php";
    //   helpMe.async = true;
    //   document.head.append(helpMe)
    // }

    // Kinda dirty hack for this file
    if (this.isElectron) {
      return;
    }

    setTimeout(() => {
      this.loadAds("ad-1", (value) => this.showAdOnePlaceholder = value, this.$refs.adRef, {size: { width: 400, height: 300 }});
      // this.loadAds("ad-2", (value) => this.showAdTwoPlaceholder = value, this.$refs.adRefSecond, {size: {width: 300, height: 250}});
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
      console.log(`[AD: ${id}]`, error)
    });
    this.ads[id].addEventListener('player_loaded', () => {
      this.logger.info(`[AD: ${id}] Player loaded`);
    });
    this.ads[id].addEventListener('display_ad_loaded', () => {
      emitPlaceholderUpdate(false);
      this.logger.info(`[AD: ${id}] Display ad loaded and ready`);
      fetch(`${process.env.VUE_APP_MODPACK_API}/public/analytics/ads/static`);
    });
    this.ads[id].addEventListener('play', () => {
      emitPlaceholderUpdate(false);
      this.logger.info(`[AD: ${id}] Ad ready and loaded`);
    });
    this.ads[id].addEventListener('complete', () => {
      this.logger.info(`[AD: ${id}] Video ad finished playing`);
    });
    this.ads[id].addEventListener('impression', () => {
      fetch(`${process.env.VUE_APP_MODPACK_API}/public/analytics/ads/video`);
    });
  }

  get advertsEnabled(): boolean {
    if (!this.auth?.token?.activePlan) {
      return true;
    }

    // If this fails, show the ads
    return (this.settings?.settings?.showAdverts === true || this.settings?.settings?.showAdverts === 'true') ?? true;
  }

  static mkRandom() {
    return Math.round(Math.random() * 999999999999);
  }

  get adZone() {
    return this.platform.isElectron() ? "4" : "1"
  }

  get isDevEnv() {
    return process.env.NODE_ENV !== 'production';
  }
}
</script>

<style lang="scss" scoped>
.ad-aside {
  padding: 1rem;
  background: rgb(17 17 17);

  min-width: calc(300px + 2.5rem); // Why? Fuck knows, Thanks css

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;

  border-left: 2px solid rgba(black, 0.1);

  .ads {
    display: flex;
    align-items: center;
    flex-direction: column;
    position: relative;
    z-index: 1;

    &.ad-container {
      width: 400px;
      &.electron {
        width: 300px;
      }
    }

    .ad-holder {
      position: relative;
      width: 400px;
      height: 300px;

      &.small {
        width: 300px;
        height: 250px;
      }

      .place-holder {
        position: absolute;
        width: 400px;
        height: 300px;
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
