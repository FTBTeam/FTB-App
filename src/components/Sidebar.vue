<template>
  <div class="sidebar small" :class="{ 'is-transparent': isTransparent }">
    <!--     <logo width="80%" class="logo" draggable="false"/>-->
    <router-link to="/">
      <img src="../assets/images/ftb-logo.svg" width="60" class="cursor-pointer logo-hover logo" draggable="false" />
    </router-link>
    <!-- <font-awesome-icon v-if="auth.token !== null && (settings.settings.enableChat === true || settings.settings.enableChat === 'true')" title="Open Friends List" class="cursor-pointer absolute text-gray-400 opacity-50 hover:opacity-100" style="left: 10px; top: 120px;" @click="openFriends()" icon="user-friends" size="md"></font-awesome-icon> -->
    <div class="nav-items nav-main mt-5">
      <router-link
        :is="disableNav ? 'span' : 'router-link'"
        v-for="(item, index) in navigation"
        :key="index"
        :to="{ name: item.to }"
        class="nav-item"
        :class="{ 'item-disabled': disableNav }"
        :aria-label="item.name"
        data-balloon-pos="right"
      >
        <div class="icon"><font-awesome-icon :icon="item.icon" class="mr-3" /></div>
        <span>{{ item.name }}</span>
      </router-link>
    </div>
    <div class="nav-items">
      <router-link
        :to="{ name: 'MTIntegration' }"
        v-if="auth.token !== null"
        class="nav-item capitalize"
        :class="{ 'item-disabled': disableNav }"
      >
        <div class="flex items-center account">
          <img
            :src="`https://api.mymcuu.id/head/${avatarName}`"
            style="width: 30px; height: 30px;"
            class="rounded-full"
          />
          <div class="flex flex-col">
            <span>{{
              auth.token.mc !== undefined && auth.token.mc.display !== null
                ? auth.token.mc.display.split('#')[0]
                : auth.token.username
            }}</span
            ><span v-if="auth.token.mc !== undefined && auth.token.mc.display !== null" class="text-xs opacity-50 hash"
              >#{{ auth.token.mc.display.split('#')[1] }}</span
            >
          </div>
        </div>
      </router-link>
    </div>
    <div aria-label="Setup a server with Creeperhost" data-balloon-pos="right" class="w-full">
      <img
        src="../assets/ch-logo.svg"
        class="my-4 mx-auto w-full cursor-pointer logo-hover"
        style="height: 30px"
        draggable="false"
        @click="openPromo()"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { AuthState } from '../modules/auth/types';
import { Action, State } from 'vuex-class';
import { SettingsState } from '../modules/settings/types';
import { logVerbose } from '../utils';
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

  private openLogin() {
    // NOTE: the callback is only used on overwolf
    platform.get.actions.openLogin((data: any) => {
      if (data.token) {
        this.setSessionID(data.token);
      }
      if (data['app-auth']) {
        let settings = this.settings?.settings;
        if (settings !== undefined) {
          settings.sessionString = data['app-auth'];
        }
        this.saveSettings(settings);
      }
    });
  }

  private debugLog(data: any) {
    logVerbose(this.settings, data);
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

  .logo {
    margin-top: 16px;
    padding-left: 0.2rem;
  }

  &.small {
    width: 70px;

    .logo {
      padding: 0 0.5rem;
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
