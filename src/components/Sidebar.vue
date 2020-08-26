<template>
  <div class="sidebar relative">
<!--     <logo width="80%" class="logo" draggable="false"/>-->
    <div class="miniftb pointer-events-none" ></div>
    <img src="../assets/logo_ftb.png" width="125px" class="cursor-pointer logo-hover z-10" @click="openFTB()" style="margin-top: 10px;"  draggable="false" />
    <font-awesome-icon v-if="isDevelop" title="Give feedback!" class="cursor-pointer absolute text-gray-400 opacity-50 hover:opacity-100" style="right: 10px; top: 100px;" @click="openGithub()" icon="comments" size="lg"></font-awesome-icon>
    <font-awesome-icon v-if="auth.token !== null " title="Open Friends List" class="cursor-pointer absolute text-gray-400 opacity-50 hover:opacity-100" style="left: 10px; top: 100px;" @click="openFriends()" icon="user-friends" size="lg"></font-awesome-icon>
    <div class="nav-items flex-col mt-5">
      <nav-item :isActive="isActiveTab('home')" @click="goTo('home')"><div class="text-right" style="width: 35px !important;"><font-awesome-icon icon="home" size="lg" class="mr-3" /></div>Home</nav-item>
      <nav-item :isActive="isActiveTab('news')" @click="goTo('news')"><div class="text-right" style="width: 35px !important;"><font-awesome-icon icon="newspaper" size="lg" class="mr-3" /></div>News</nav-item>
      <nav-item :isActive="isActiveTab('discover')" @click="goTo('discover')"><div class="text-right" style="width: 35px !important;"><font-awesome-icon icon="globe-europe" size="lg" class="mr-3" /></div>Discover</nav-item>
      <nav-item :isActive="isActiveTab('browseModpacks')" @click="goTo('browseModpacks')"><div class="text-right" style="width: 35px !important;"><font-awesome-icon icon="search" size="lg" class="mr-3" /></div>Browse</nav-item>
      <nav-item :isActive="isActiveTab('modpacks')" @click="goTo('modpacks')" class="text-left"><div class="text-right" style="width: 35px !important;"><font-awesome-icon icon="box-open" size="lg" class="mr-3"/></div>My Modpacks</nav-item>
    </div>
    <div class="nav-items flex-col mt-auto mb-0">
      <nav-item :isActive="isActiveTab('settings')" @click="goTo('settings')"><font-awesome-icon icon="cog" size="lg" class="mr-3" />Settings</nav-item>
      <nav-item v-if="auth.token === null" @click="openLogin()"><font-awesome-icon icon="sign-out-alt" size="lg" class="mr-3" />Login</nav-item>
      <nav-item v-else class="capitalize" @click="goTo('profile')"><img :src="`https://minotar.net/helm/${auth.token.accounts.find((s) => s.identityProvider === 'mcauth').userId}`" style="margin-right: 0.75em;" width="21px" class="rounded-full" />{{auth.token.mc.display}}</nav-item>
    </div>
    <img src="../assets/ch-logo.svg" width="90%" class="mb-2 cursor-pointer logo-hover" style="" draggable="false" @click="openPromo()"/>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import NavItem from './nav/NavItem.vue';
import {shell, ipcRenderer} from 'electron';
import {AuthState} from '../modules/auth/types';
import {State, Action} from 'vuex-class';
import store from '@/store';
import config from '@/config';
import { SettingsState } from '../modules/settings/types';
import { logVerbose } from '../utils';

@Component({components: {NavItem}})
export default class Sidebar extends Vue {
  @State('auth')
  private auth!: AuthState;
  @State('settings')
  private settings!: SettingsState;
  private appVersion: string = config.appVersion;


  get isDevelop() {
    const splits = this.appVersion.split('-');
    if (splits.length === 0) {
      return true;
    }
    return !splits[splits.length - 1].match(/\d/);
  }


  public isActiveTab(tab: string): boolean {
    return tab === 'home' && this.$route.path === '/' ? true : this.$route.path.startsWith(`/${tab}`);
  }

  public goTo(page: string): void {
    // We don't care about this error!
    this.$router.push({name: page}).catch((err) => { return; });
  }

  public openPromo(): void {
    shell.openExternal('https://creeperhost.net/applyPromo/FEEDME');
  }

  public openFTB(): void {
    shell.openExternal('https://feed-the-beast.com');
  }

  public openGithub(): void {
    shell.openExternal('https://github.com/FTBTeam/FTB-App/issues');
  }

  public openFriends() {
    // this.$router.push({name: 'profile'})
    ipcRenderer.send('showFriends');
  }

  private openLogin() {
    ipcRenderer.send('openOauthWindow');
  }

  private debugLog(data: any) {
    logVerbose(this.settings, data);
  }
}
</script>

<style scoped lang="scss">
    .sidebar {
        height: 100%;
        flex: 1;
        background-color: var(--color-navbar);
        display: flex;
        align-items: center;
        justify-content: flex-start;
        flex-direction: column;
    }
    .logo {
      margin-top: 10px;
      margin-bottom: 20px;
    }
    .nav-items {
      width: 100%;
      display: flex;
      cursor: pointer;
    }
  .miniftb{
    background-image: url("../assets/ftb-tiny-desat.png");
    width: 219px;
    height: 190px;
    top: -30px;
    position: absolute;
    text-align: center;
    filter: brightness(0.7);
    -webkit-mask-image: -webkit-gradient(linear, left top, left bottom, from(rgba(0,0,0,0.4)), to(rgba(0,0,0,0)));
  }
  .logo-hover {
    transition: all .2s ease;
  }
  .logo-hover:hover {
    filter: drop-shadow(1px 1px 2px #000000);
  }
</style>
