<template>
  <div class="sidebar small" :class="{ 'is-transparent': isTransparent }">
    <sidebar-create :disabled="disableNav" />
    
    <div class="nav-items nav-main mt-2">      
      <popover :text="item.name" v-for="(item, index) in navigation" :key="index">
        <router-link :to="{ name: item.to }">
          <div class="nav-item" :class="{ 'item-disabled': disableNav }" @click.right="e => navItemRightClick(e, item)">
            <div class="icon"><font-awesome-icon :icon="item.icon" class="mr-3" /></div>
          </div>
        </router-link>
      </popover>
    </div>

    <div class="nav-items">      
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
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import {Action} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import SidebarProfile from '@/components/layout/sidebar/SidebarProfile.vue';
import {RouterNames} from '@/router';
import SidebarCreate from '@/components/layout/sidebar/SidebarCreate.vue';
import {AppContextController} from '@/core/context/contextController';
import {ContextMenus} from '@/core/context/contextMenus';

@Component({
  components: {SidebarCreate, SidebarProfile },
})
export default class Sidebar extends Vue {
  @Prop({ default: false }) isTransparent!: boolean;

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
    platform.get.utils.openUrl('https://go.ftb.team/ch-a-i');
  }

  navItemRightClick(event: PointerEvent, item: typeof this.navigation[0]) {
    if (item.to === RouterNames.SETTINGS_INSTANCE) {
      AppContextController.openMenu(ContextMenus.NAV_SETTINGS_MENU, event, {});
    }
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
