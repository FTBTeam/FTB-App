<template>
  <div class="app-integrations">
    <h3 class="text-xl mb-4 font-bold">Active integrations</h3>
    <div class="active-integrations integrations mb-8">
      <p v-if="auth.token === null && !auth.loggingIn">
        Looks like you've not got any integrations setup, use the 'available integrations' section below to get started.
      </p>

      <div class="card" v-else>
        <div class="logo"><img src="@/assets/images/mt-logo.webp" alt="" /></div>
        <div class="main" v-if="!auth.loggingIn">
          <div class="name font-bold mb-2 flex items-center">
            <div class="avatar mr-2">
              <img
                :src="getMinecraftHead(avatarName)"
                style="margin-right: 0.75em; width: 40px; height: 40px"
                class="rounded-full"
              />
            </div>
            <div class="name flex flex-col">
              <span class="block">{{
                auth.token.mc !== undefined && auth.token.mc.display !== null
                  ? auth.token.mc.display.split('#')[0]
                  : auth.token.username
              }}</span
              ><span
                v-if="auth.token.mc !== undefined && auth.token.mc.display !== null"
                class="text-xs opacity-50 hash"
                >#{{ auth.token.mc.display.split('#')[1] }}</span
              >
            </div>
          </div>
          <div class="setup inline-block mt-4 text-sm">
            <ui-button :wider="true" type="success" @click="logout" icon="sign-out">Logout</ui-button>
          </div>
        </div>
        <div class="main flex" v-else>
          <font-awesome-icon icon="spinner" spin class="mr-3" />
          <span>Loading....</span>
        </div>
      </div>
    </div>

    <h3 class="text-xl font-bold mb-4" v-if="auth.token === null">Available integrations</h3>
    <div class="integrations" v-if="auth.token === null">
      <div class="card" :class="{ disabled: auth.loggingIn }">
        <div class="logo"><img src="@/assets/images/mt-logo.webp" alt="" /></div>
        <div class="main">
          <div class="name font-bold mb-1">MineTogether</div>
          <div class="desc opacity-75">Integrate with MineTogether to use cloudsaves and private packs.</div>
          <div class="setup inline-block mt-4 text-sm">
            <ui-button :wider="true" type="success" @click="openLogin" icon="sign-in">Login to setup</ui-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import platform from '@/utils/interface/electron-overwolf';
import {SettingsState} from '@/modules/settings/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';

@Component({
  components: {UiButton},
  methods: {getMinecraftHead}
})
export default class AppInfo extends Vue {
  @State('auth') private auth!: AuthState;
  @State('settings') private settings!: SettingsState;
  @Action('logout', { namespace: 'auth' }) private logoutAction!: () => void;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('setSessionID', { namespace: 'auth' }) private setSessionID!: any;

  get avatarName() {
    const provider = this.auth.token?.accounts.find((s) => s.identityProvider === 'mcauth');
    return provider !== undefined && provider !== null ? provider.userId : 'MHF_Steve';
  }

  private openLogin() {
    platform.get.actions.openLogin((data: { token: string; 'app-auth': string } | null) => {
      if (data === null) {
        return;
      }

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

  public logout() {
    this.logoutAction();
    // get instances and store
    sendMessage("ircQuitRequest", {})

    platform.get.actions.logoutFromMinetogether();

    this.settings.settings.sessionString = undefined;
    this.saveSettings(this.settings.settings);
  }
}
</script>

<style scoped lang="scss">
.integrations {
  .card {
    display: flex;
    align-items: center;

    background: var(--color-background);
    padding: 1rem;
    border-radius: 5px;
    position: relative;

    &.disabled {
      pointer-events: none;

      & > div {
        opacity: 0.2;
        filter: blur(2px) grayscale(1);
      }

      &::before {
        content: 'Integration active';
        top: 0;
        left: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        //background: rgba(black, 0.5);
        position: absolute;
        border-radius: 5px;
        z-index: 2;
        font-style: italic;
      }
    }

    .logo {
      width: 120px;
      margin-right: 2rem;
      padding: 1rem;
    }
  }
}
</style>
