<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';
import SidebarProfile from '@/components/layout/sidebar/SidebarProfile.vue';
import {RouterNames} from '@/router';
import SidebarCreate from '@/components/layout/sidebar/SidebarCreate.vue';
import {AppContextController} from '@/core/context/contextController';
import {ContextMenus} from '@/core/context/contextMenus';
import SidebarRunningInstances from '@/components/layout/sidebar/SidebarRunningInstances.vue';
import { ref } from 'vue';
import Popover from '@/components/ui/Popover.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

// TODO: [port] Fix this
// @Action('openSignIn', { namespace: 'core' }) public openSignIn: any;
const openSignIn = ref(false);

const navigation = [
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
    to: RouterNames.SETTINGS_APP,
    icon: 'cog',
  },
];

function openPromo(): void {
  platform.get.utils.openUrl('https://bisecthosting.com/ftb?r=app-icon');
}

function navItemRightClick(event: PointerEvent, item: typeof navigation[0]): void {
  if (item.to === RouterNames.SETTINGS_APP) {
    AppContextController.openMenu(ContextMenus.NAV_SETTINGS_MENU, event, () => {});
  }
}
</script>

<template>
  <div class="sidebar small">
    <sidebar-create />
    
    <div class="nav-items nav-main mt-2">      
      <Popover :text="item.name" v-for="(item, index) in navigation" :key="index">
        <RouterLink :to="{ name: item.to }">
          <div class="nav-item" @click.right="e => navItemRightClick(e, item)">
            <div class="icon"><FontAwesomeIcon :icon="item.icon" class="mr-3" /></div>
          </div>
        </RouterLink>
      </Popover>

      <SidebarRunningInstances />
    </div>

    <div class="nav-items">
      <SidebarProfile class="block" @signin="openSignIn()" />
    </div>
    
    <Popover text="Setup a server with BisectHosting" class="w-full">
      <img
        @click="openPromo"
        src="../../../assets/images/branding/bh-logo.svg"
        class="my-4 mx-auto w-full cursor-pointer logo-hover"
        style="height: 36px"
        draggable="false"
        alt="Bisect Hosting Logo"
      />
    </Popover>
  </div>
</template>

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
