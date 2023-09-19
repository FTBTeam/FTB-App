<template>
  <div class="sidebar small" :class="{ 'is-transparent': isTransparent, 'is-dev': isDev }">
    <router-link to="/" :class="{ 'item-disabled': disableNav }">
      <img
        src="../../../assets/images/ftb-logo.svg"
        width="60"
        alt="FTB Logo"
        class="cursor-pointer logo-hover logo"
        draggable="false"
      />
    </router-link>
    <div class="nav-items nav-main mt-5">
      <popover :text="item.name" v-for="(item, index) in navigation" :key="index">
        <router-link :to="{ name: item.to }" class="nav-item" :class="{ 'item-disabled': disableNav }">
          <div class="icon"><font-awesome-icon :icon="item.icon" class="mr-3" /></div>
        </router-link>
      </popover>
    </div>

    <div class="nav-items">
<!--      <a-->
<!--        v-if="auth.token !== null"-->
<!--        class="nav-item capitalize"-->
<!--        :class="{ 'item-disabled': disableNav }"-->
<!--        @click="openFriends"-->
<!--      >-->
<!--        <div class="icon"><font-awesome-icon icon="user-friends" class="mr-3" /></div>-->
<!--        <span class="whitespace-no-wrap">Friends List</span>-->
<!--      </a>-->

      <sidebar-profile class="block" :disable="disableNav" @signin="openSignIn({ open: true })" />
    </div>
    <popover text="Setup a server with CreeperHost" class="w-full">
      <img
        @click="openPromo"
        src="../../../assets/ch-logo.svg"
        class="my-4 mx-auto w-full cursor-pointer logo-hover"
        style="height: 30px"
        draggable="false"
        alt="CreeperHost Logo"
      />
    </popover>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { AuthState } from '@/modules/auth/types';
import { Action, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import SidebarProfile from '@/components/layout/sidebar/SidebarProfile.vue';
import { RouterNames } from '@/router';
import { yeetError } from '@/utils';

@Component({
  components: { SidebarProfile },
})
export default class Sidebar extends Vue {
  @State('auth') private auth!: AuthState;
  @State('settings') private settings!: SettingsState;
  @Prop({ default: false }) isTransparent!: boolean;
  @Prop({ default: false }) isDev!: boolean;

  @Action('openSignIn', { namespace: 'core' }) public openSignIn: any;

  currentRoute: string | null | undefined = '';

  mounted() {
    this.currentRoute = this.$route.name;
  }

  navigation = [
    {
      name: 'Home',
      to: RouterNames.HOME,
      icon: 'home',
    },
    {
      name: 'Library',
      to: RouterNames.ROOT_LIBRARY,
      icon: 'book-open',
    },
    {
      name: 'Browse',
      to: RouterNames.ROOT_BROWSE_PACKS,
      icon: 'search',
    },
    // {
    //   name: 'Discover',
    //   to: RouterNames.ROOT_DISCOVER,
    //   icon: 'globe-europe',
    // },
    {
      name: 'Blog',
      to: RouterNames.ROOT_BLOG,
      icon: 'rss',
    },
    {
      name: 'Support',
      to: RouterNames.SUPPORT,
      icon: 'info-circle',
    },
    {
      name: 'Settings',
      to: RouterNames.SETTINGS_INSTANCE,
      icon: 'cog',
    },
  ];

  @Watch('$route.name')
  onRouteChange(value: string) {
    this.currentRoute = value;
  }

  get disableNav() {
    return this.currentRoute === RouterNames.ROOT_LAUNCH_PACK;
  }

  public openPromo(event: any): void {
    platform.get.utils.openUrl('https://go.ftb.team/ch-app');
  }

  // public openFriends() {
  //   if (this.disableNav) {
  //     return;
  //   }
  //   platform.get.actions.openFriends();
  // }
}
</script>

<style scoped lang="scss">
.sidebar {
  position: relative;
  width: 190px;
  height: 100%;
  background-color: var(--color-navbar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-direction: column;
  transition: background-color 0.3s ease-in-out;

  &.is-dev {
    background-color: #171c1f;
  }

  .item-disabled {
    opacity: 0.1;
    cursor: not-allowed;
    pointer-events: none;

    &:hover {
      background: inherit !important;
    }
  }

  .item-disabled .logo {
    filter: grayscale(1);
  }

  .logo {
    margin-top: 16px;
    padding-left: 0.2rem;
  }

  &.small {
    width: 70px;

    .logo {
      padding: 0 0.5rem;
      margin-bottom: 0.5rem;
    }

    .nav-items .nav-item {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem 0;

      .icon {
        width: auto !important;
        font-size: 18px;

        svg {
          max-width: unset !important;
          margin: 0;
        }
      }

      span {
        display: none;
      }
    }
  }
}

.nav-main {
  flex: 1;
}

.nav-items {
  width: 100%;

  .nav-item {
    display: block;
    cursor: pointer;
    padding: 0.5rem 1rem;
    transition: background-color 0.25s ease-in-out;

    .icon {
      display: inline-block;
      width: 35px;

      svg {
        max-width: 16px;
      }
    }

    &.router-link-exact-active {
      background: #2a2a2a;
    }

    &:hover {
      background: #202020;
    }
  }
}

.account .hash {
  letter-spacing: 0.07rem;
}

.logo-hover {
  transition: filter 0.2s ease, transform 0.2s ease;
}

.logo-hover:hover {
  filter: drop-shadow(0px 0px 15px #00000091);
  transform: translateY(-3px);
}
</style>
