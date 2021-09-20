<template>
  <div class="ad-aside">
    <div class="heading">
      <nav>
        <p class="font-bold text-lg mb-4">Helpful links</p>
        <div class="flex">
          <div
            class="item up"
            aria-label="FTB Discord"
            data-balloon-pos="down-left"
            @click="() => platform.get.utils.openUrl('https://ftb.team/discord')"
          >
            <font-awesome-icon :icon="['fab', 'discord']" />
          </div>

          <div
            class="item up"
            aria-label="FTB Twitter"
            data-balloon-pos="down-left"
            @click="() => platform.get.utils.openUrl('https://twitter.com/FTB_Team')"
          >
            <font-awesome-icon :icon="['fab', 'twitter']" />
          </div>

          <div
            class="item up"
            aria-label="FTB Twitch"
            data-balloon-pos="down-left"
            @click="() => platform.get.utils.openUrl('https://twitch.tv/ftb')"
          >
            <font-awesome-icon :icon="['fab', 'twitch']" />
          </div>
        </div>

        <p class="font-bold text-lg mt-6 mb-2">Support</p>
        <div class="item" @click="() => platform.get.utils.openUrl('https://creeperhost.net/applyPromo/FEEDME')">
          Order a server from Creeperhost
        </div>
        <div class="item" @click="() => platform.get.utils.openUrl('https://feed-the-beast.com/support')">
          Need help using the app?
        </div>
        <div class="item" @click="() => platform.get.utils.openUrl('https://github.com/FTBTeam/FTB-App-Feedback')">
          Report an app bug
        </div>
        <div
          class="item"
          @click="
            () =>
              platform.get.utils.openUrl(
                'https://feedthebeast.notion.site/How-do-I-setup-my-own-server-43b4c6b83f264dc6b2fb7a9566864ab0',
              )
          "
        >
          Server setup guide
        </div>
        <div
          class="item"
          @click="
            () =>
              platform.get.utils.openUrl('https://feedthebeast.notion.site/FTB-App-cb0a0cfc8acc490b99290c21591c29dc')
          "
        >
          App Guides
        </div>
      </nav>
    </div>

    <div class="ad-space">
      <div class="heart text-center mb-4" v-if="!isElectron">
        <div class="heart-hearts">
          <font-awesome-icon icon="heart" size="2x" class="mb-2" />
          <font-awesome-icon icon="heart" size="2x" class="less-than-three heart-2 mb-2" />
          <font-awesome-icon icon="heart" size="2x" class="less-than-three heart-3 mb-2" />
        </div>
        <p class="font-bold">Supports FTB & Curseforge Authors</p>
      </div>
      <div class="ad-box" :class="{ overwolf: !isElectron }">
        <div class="flex flex-col w-full mt-auto mb-auto" v-if="advertsEnabled">
          <div
            v-if="!showPlaceholder && !isElectron"
            id="ow-ad"
            ref="adRef"
            style="max-width: 400px; max-height: 300px; display: flex; margin: 0 auto;"
          >
            <!--            <div v-if="platform.isElectron()" id="777249406"></div>-->
          </div>
          <video
            @click="platform.get.utils.openUrl('https://creeperhost.net/applyPromo/FEEDME')"
            class="cursor-pointer"
            width="400"
            height="300"
            autoplay
            muted
            loop
            style="margin: 0 auto"
            v-if="isElectron"
          >
            <source src="https://dist.modpacks.ch/windows_desktop_src_assets_CH_AD.mp4" type="video/mp4" />
          </video>
        </div>
      </div>
      <small
        class="text-xs text-center block cursor-pointer mt-2 text-muted hover:text-white hover:shadow"
        @click="reportAdvert"
        v-if="!isElectron"
        >Report advert</small
      >
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import Component from 'vue-class-component';
import { Action, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';

/**
 * TODO: clean all of this up, it's a mess and a copy of a different component
 */
@Component
export default class AdAside extends Vue {
  @State('settings') public settings!: SettingsState;
  @State('auth') public auth!: AuthState;
  @Action('reportAdvert') public reportAd!: any;

  private ad: any;
  platform = platform;
  showPlaceholder = false;

  private starAPI = (window as any).cpmstarAPI;

  get isElectron() {
    return platform.isElectron();
  }

  async mounted() {
    // Kinda dirty hack for this file
    if (this.platform.isElectron()) {
      // if (this.advertsEnabled) {
      //   setTimeout(() => {
      //     this.addAdvert();
      //     // this.ad.addEventListener('error', () => {
      //     //   this.showPlaceholder = true;
      //     // });
      //     // this.ad.addEventListener('impression', () => {
      //     //   fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/video`);
      //     // });
      //     // this.ad.addEventListener('display_ad_loaded', () => {
      //     //   fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/${this.$route.query.modpackid}/${this.$route.query.versionID}/ad/install/static`);
      //     // });
      //   }, 500);
      // }
    } else {
      setTimeout(() => {
        console.log('Loading advert');
        //@ts-ignore
        if (!OwAd) {
          this.showPlaceholder = true;
          console.log('No advert loaded');
        } else {
          //@ts-ignore
          if (window.ad) {
            console.log('Is window advert');
            //@ts-ignore
            this.ad = window.ad;
            this.ad.refreshAd();
          } else {
            console.log('Created advert');
            //@ts-ignore
            this.ad = new OwAd(document.getElementById('ow-ad'));
            //@ts-ignore
            window.ad = this.ad;
          }
          this.ad.addEventListener('error', () => {
            this.showPlaceholder = true;
          });
          this.ad.addEventListener('impression', () => {
            fetch(`${process.env.VUE_APP_MODPACK_API}/public/analytics/ads/video`);
          });
          this.ad.addEventListener('display_ad_loaded', () => {
            fetch(`${process.env.VUE_APP_MODPACK_API}/public/analytics/ads/static`);
          });
        }
      }, 500);
    }
  }

  public addAdvert() {
    try {
      this.starAPI((api: { game: { setTarget: (e: unknown) => void } }) => {
        api.game.setTarget(document.getElementById('ad'));
      });
      // @ts-ignore
      this.starAPI({
        kind: 'game.createInterstitial',
        fail: () => {
          console.log('API was blocked or failed to load');
          this.showPlaceholder = true;
        },
      });
      // @ts-ignore
      this.starAPI({
        kind: 'game.displayInterstitial',
        onAdOpened() {
          console.log('Interstitial opened');
        },
        onAdClosed: () => {
          this.show300x250();
        },
        fail: () => {
          this.show300x250();
        },
      });
    } catch (error) {
      this.showPlaceholder = true;
    }
  }

  public show300x250() {
    const el = document.getElementById('ad');
    this.starAPI({ kind: 'go', module: 'banner300x250', config: { target: { el, kind: 'replace' } } });
  }

  get advertsEnabled(): boolean {
    return (
      this.settings?.settings?.showAdverts === true ||
      this.settings?.settings?.showAdverts === 'true' ||
      this.auth?.token?.activePlan === null
    );
  }
  public reportAdvert() {
    const el = document.getElementById('banner300x250');
    if (!el) {
      this.showPlaceholder = true;
      return;
    }
    // @ts-ignore
    const adHTML = el.children[0].contentDocument.body.innerHTML;
    // @ts-ignore
    this.starAPI(api => {
      api.game.setTarget(null);
    });
    el.innerHTML = '';
    this.ad = null;
    // @ts-ignore
    window.ad = null;
    this.showPlaceholder = true;
    this.reportAd({ object: '', html: adHTML });
  }
}
</script>

<style lang="scss" scoped>
.ad-aside {
  padding: 1.5rem;
  background: rgba(black, 0.5);

  display: flex;
  flex-direction: column;

  justify-content: space-between;

  .heading {
    nav {
      .item {
        padding: 0.25rem 0;
        opacity: 0.75;

        cursor: pointer;
        transition: opacity 0.25s ease-in-out, transform 0.25s ease-in-out;

        &.up:hover {
          transform: translateY(-3px);
        }

        &:hover {
          opacity: 1;
          transform: translateX(5px);
        }

        svg {
          margin-right: 1rem;
        }
      }
    }
  }

  .ad-space {
    .heart {
      position: relative;
      svg {
        color: #ff4040;
      }

      .less-than-three {
        position: absolute;
        left: 50%;
        transform: translateX(-50%) scale(1.5);
        opacity: 0;

        animation: scale 2s ease-in-out infinite;

        &.heart-3 {
          animation: scale2 2s ease-in-out infinite;
          animation-delay: 0.4s;
        }

        @keyframes scale {
          0%,
          100% {
            transform: translateX(-50%) scale(1);
            opacity: 0;
          }
          50% {
            transform: translateX(-50%) scale(1.5);
            opacity: 0.6;
          }
        }

        @keyframes scale2 {
          0%,
          100% {
            transform: translateX(-50%) scale(1);
            opacity: 0;
          }
          50% {
            transform: translateX(-50%) scale(1.8);
            opacity: 0.2;
          }
        }
      }
    }

    .ad-box {
      background: black;
      width: 300px;
      height: 250px;
      overflow: hidden;
      display: flex;
      align-items: center;

      border-radius: 5px;

      &.overwolf {
        width: 400px;
        height: 300px;
      }
    }
  }
}
</style>
