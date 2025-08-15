<script lang="ts" setup>
import appPlatform from '@platform';
import SidebarProfile from '@/components/layout/sidebar/SidebarProfile.vue';
import {RouterNames} from '@/router';
import SidebarCreate from '@/components/layout/sidebar/SidebarCreate.vue';
import {AppContextController} from '@/core/context/contextController';
import {ContextMenus} from '@/core/context/contextMenus';
import SidebarRunningInstances from '@/components/layout/sidebar/SidebarRunningInstances.vue';
import Popover from '@/components/ui/Popover.vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faBookOpen, faCode, faCog, faHome, faInfoCircle, faRss, faSearch} from '@fortawesome/free-solid-svg-icons';
import {constants} from '@/core/constants.ts';
import InstallQueue from "@/components/groups/modpack/InstallQueue/InstallQueue.vue";

const navigation = [
  {
    name: 'Home',
    to: RouterNames.HOME,
    icon: faHome,
  },
  {
    name: 'Library',
    to: RouterNames.ROOT_LIBRARY,
    icon: faBookOpen,
  },
  {
    name: 'Browse',
    to: RouterNames.ROOT_BROWSE_PACKS,
    icon: faSearch,
  },
  {
    name: 'Blog',
    to: RouterNames.ROOT_BLOG,
    icon: faRss,
  },
  {
    name: 'Support',
    to: RouterNames.SUPPORT,
    icon: faInfoCircle,
  },
  {
    name: 'Settings',
    to: RouterNames.SETTINGS_APP,
    icon: faCog,
  },
];

if (constants.isDevelopment) {
  navigation.push({
    name: 'Dev',
    to: RouterNames.DEV_HOME,
    icon: faCode,
  });
}

function openPromo(): void {
  appPlatform.utils.openUrl('https://bisecthosting.com/ftb?r=app-icon');
}

function navItemRightClick(event: MouseEvent, item: typeof navigation[0]): void {
  if (item.to === RouterNames.SETTINGS_APP) {
    AppContextController.openMenu(ContextMenus.NAV_SETTINGS_MENU, event, () => {
    });
  }
}
</script>

<template>
  <div class="sidebar small">
    <sidebar-create/>

    <div>
      <InstallQueue/>
    </div>

    <div class="nav-items nav-main mt-2">
      <Popover :text="item.name" v-for="(item, index) in navigation" :key="index">
        <RouterLink :to="{ name: item.to }" :draggable="false">
          <div class="nav-item" @click.right="(e) => navItemRightClick(e, item)">
            <div class="icon">
              <FontAwesomeIcon :fixed-width="true" :icon="item.icon" class="mr-3"/>
            </div>
          </div>
        </RouterLink>
      </Popover>

      <SidebarRunningInstances/>
    </div>

    <div class="nav-items">
      <SidebarProfile class="block"/>
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
  width: 60px;
  height: 100%;
  background-color: var(--color-navbar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-direction: column;
  transition: background-color 0.3s ease-in-out;

  .logo {
    padding: 0 0.5rem;
    margin-bottom: 0.5rem;
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
}

.nav-main {
  flex: 1;
}

.nav-items {
  display: flex;
  flex-direction: column;
  gap: .8rem;
  
  .router-link-exact-active, .router-link-active {
    .nav-item {
      background: var(--color-green-600) !important;
    }
  }

  .nav-item {
    cursor: pointer;
    transition: background-color 0.25s ease-in-out;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: .4rem .6rem;
    border-radius: 6px;

    .icon {
      width: auto !important;
      font-size: 1.4rem;

      svg {
        max-width: unset !important;
        margin: 0;
      }
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
