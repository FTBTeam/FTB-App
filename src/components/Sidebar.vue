<template>
  <div class="sidebar small" :class="{ 'is-transparent': isTransparent }">
    <!--     <logo width="80%" class="logo" draggable="false"/>-->
    <router-link to="/" :is="disableNav ? 'span' : 'router-link'" :class="{ 'item-disabled': disableNav }">
      <img src="../assets/images/ftb-logo.svg" width="60" class="cursor-pointer logo-hover logo" draggable="false" />
    </router-link>
    <div class="nav-items nav-main mt-5">
      <router-link
        :is="disableNav ? 'span' : 'router-link'"
        v-for="(item, index) in navigation"
        :key="index"
        :to="{ name: item.to }"
        class="nav-item"
        :class="{ 'item-disabled': disableNav }"
      >
        <div class="icon"><font-awesome-icon :icon="item.icon" class="mr-3" /></div>
        <span>{{ item.name }}</span>
      </router-link>
    </div>
    <div class="nav-items">
      <!-- <font-awesome-icon v-if="auth.token !== null && (settings.settings.enableChat === true || settings.settings.enableChat === 'true')" title="Open Friends List" class="cursor-pointer absolute text-gray-400 opacity-50 hover:opacity-100" style="left: 10px; top: 120px;" @click="openFriends()" icon="user-friends" size="md"></font-awesome-icon> -->
      <a
        v-if="auth.token !== null"
        class="nav-item capitalize"
        :class="{ 'item-disabled': disableNav }"
        @click="openFriends"
      >
        <div class="icon"><font-awesome-icon icon="user-friends" class="mr-3" /></div>
        <span class="whitespace-no-wrap">Friends List</span>
      </a>
      <router-link
        :to="{ name: 'MTIntegration' }"
        class="nav-item capitalize"
      >
        <div class="flex items-center account">
          <img
            :src="`https://api.mymcuu.id/head/${getActiveProfile.uuid}`"
            style="width: 40px; height: 40px;"
            class="rounded"
          />
        </div>
      </router-link>
    </div>
    <div aria-label="Setup a server with CreeperHost" data-balloon-pos="right" class="w-full" @click="openPromo()">
      <img
        src="../assets/ch-logo.svg"
        class="my-4 mx-auto w-full cursor-pointer logo-hover"
        style="height: 30px"
        draggable="false"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { AuthState } from '@/modules/auth/types';
import { Action, Getter, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';
import { ModpackState } from '@/modules/modpacks/types';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class Sidebar extends Vue {
  @State('auth') private auth!: AuthState;
  @State('modpacks') private modpacks!: ModpackState;
  @State('settings') private settings!: SettingsState;
  @Action('setSessionID', { namespace: 'auth' }) private setSessionID!: any;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;

  @Prop({ default: false }) isTransparent!: boolean;

  @Getter('getProfiles', { namespace: 'core' }) private getProfiles!: any;
  @Getter('getActiveProfile', { namespace: 'core' }) private getActiveProfile!: any;

  private appVersion: string = platform.get.config.appVersion;

  navigation = [
    {
      name: 'Home',
      to: 'home',
      icon: 'home',
    },
    {
      name: 'Library',
      to: 'modpacks',
      icon: 'book-open',
    },
    {
      name: 'Browse',
      to: 'browseModpacks',
      icon: 'search',
    },
    {
      name: 'Discover',
      to: 'discover',
      icon: 'globe-europe',
    },
    {
      name: 'News',
      to: 'news',
      icon: 'newspaper',
    },
    {
      name: 'Settings',
      to: 'instance-settings',
      icon: 'cog',
    },
  ];

  get isDevelop() {
    const splits = this.appVersion.split('-');
    if (splits.length === 0) {
      return true;
    }
    return !splits[splits.length - 1].match(/\d/);
  }

  get disableNav() {
    return (
      this.$router.currentRoute.path.startsWith('/launching') ||
      (this.modpacks.installing !== null && !this.modpacks.installing.error)
    );
  }

  get avatarName() {
    const provider = this.auth.token?.accounts.find(s => s.identityProvider === 'mcauth');
    return provider !== undefined && provider !== null ? provider.userId : 'MHF_Steve';
  }

  public openPromo(): void {
    platform.get.utils.openUrl('https://creeperhost.net/applyPromo/FEEDME');
  }

  public openGithub(): void {
    platform.get.utils.openUrl('https://github.com/FTBTeam/FTB-App-Overwolf/issues');
  }

  public openFriends() {
    platform.get.actions.openFriends();
  }
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

  .item-disabled .logo {
    filter: grayscale(1);
    opacity: 0.5;
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
      //margin-bottom: 1rem;
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

    a.nav-item {
      &:hover {
        span {
          left: 120%;
          opacity: 1 !important;
          visibility: visible;
        }
      }

      // This is needed because the normal way of doing this keeps it open when you navigate, super annoying
      span {
        display: block !important;
        position: absolute;
        left: 110%;
        z-index: 30;
        font-size: var(--balloon-font-size);
        background: black;
        padding: 0.5em 1em;
        border-radius: 2px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans',
          'Helvetica Neue', sans-serif;
        transition: 0.15s ease-in-out left, 0.15s ease-in-out visibility, 0.15s ease-in-out opacity;
        visibility: hidden;
        opacity: 0 !important;

        &::before {
          position: absolute;
          content: '';
          background-color: black;
          top: 50%;
          transform: translateY(-50%) rotateZ(45deg);
          left: -4px;
          border-radius: 2px;
          width: 8px;
          height: 8px;
        }
      }
    }
  }

  //&::after {
  //  content: '';
  //  background: url('../assets/ftb-tiny-desat.png') center top;
  //  width: 100%;
  //  height: 130px;
  //  position: absolute;
  //  opacity: 0.3;
  //  mask-image: -webkit-gradient(linear, left top, left bottom, from(rgba(0, 0, 0, 0.3)), to(rgba(0, 0, 0, 0)));
  //}
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

    &.item-disabled {
      opacity: 0.1;
      cursor: not-allowed;

      &:hover {
        background: inherit !important;
      }
    }
  }
}

.account .hash {
  font-family: Arial, Helvetica, sans-serif;
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
