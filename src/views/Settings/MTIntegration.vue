<template>
  <div class="minetogether-profile">
    <div class="avatar sm:flex items-center mb-16">
      <div class="account-info flex items-center">
        <img :src="`https://api.mymcuu.id/head/${avatarName}`" width="80" height="80" class="rounded-full mr-6" />
        <div class="name">
          <p class="text-lg bold">{{ auth.token.mc !== undefined ? auth.token.mc.display : auth.token.username }}</p>
          <p class="opacity-75">
            {{
              auth.token.activePlan !== undefined && auth.token.activePlan !== null
                ? auth.token.activePlan.name
                : 'No paid plan'
            }}
          </p>
        </div>
      </div>
      <div class="ml-auto mt-10 sm:mt-0 flex items-center">
        <ftb-button color="primary" class="text-center px-4 py-2 mr-2" title="Coming Soon" @click="openProfile">
          Update account
          <font-awesome-icon icon="external-link-square-alt" class="ml-2" />
        </ftb-button>
        <ftb-button color="warning" class="text-center px-4 py-2" @click="logout">Logout</ftb-button>
      </div>
    </div>

    <div class="">
<!--      <ftb-toggle-->
<!--        label="Automatically open friends list"-->
<!--        :value="settings.settings.autoOpenChat === true || settings.settings.autoOpenChat === 'true'"-->
<!--        onColor="bg-primary"-->
<!--        @change="toggleAutoOpenChat"-->
<!--        :disabled="true"-->
<!--        class="mb-8"-->
<!--        small="When enabled, the MineTogether friends list will automatically open in a new window on startup. Currently disabled."-->
<!--      />-->
      
      <ftb-toggle
        label="Enable cloud save uploads "
        :value="settings.settings.cloudSaves === true || settings.settings.cloudSaves === 'true'"
        @change="toggleCloudSaves"
        :disabled="auth.token?.activePlan === null"
        onColor="bg-primary"
        class="mb-8"
        small="Stores your worlds, configs and more in the cloud and syncs them across systems."
      />
      
      <ftb-toggle
        label="Show adverts"
        :value="settings.settings.showAdverts === true || settings.settings.showAdverts === 'true'"
        @change="toggleAdverts"
        :disabled="!auth?.token?.activePlan"
        onColor="bg-primary"
        class="mb-8"
        small="Any paid plan can optionally disable ads throughout the app."
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import { Action, State } from 'vuex-class';
import { AuthState } from '@/modules/auth/types';
import { SettingsState } from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import { gobbleError } from '@/utils/helpers/asyncHelpers';
import { sendMessage } from '@/core/websockets/websocketsApi';

@Component({
  components: {
    'ftb-toggle': FTBToggle,
  },
})
export default class MTIntegration extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('auth') private auth!: AuthState;
  @State('settings') private settings!: SettingsState;
  @Action('logout', { namespace: 'auth' }) private logoutAction!: () => void;

  get avatarName() {
    const provider = this.auth.token?.accounts.find((s) => s.identityProvider === 'mcauth');
    return provider !== undefined && provider !== null ? provider.userId : 'MHF_Steve';
  }

  public toggleCloudSaves(value: boolean) {
    this.settings.settings.cloudSaves = value;
    this.saveSettings(this.settings.settings);
  }

  public toggleAutoOpenChat(value: boolean) {
    this.settings.settings.autoOpenChat = value;
    this.saveSettings(this.settings.settings);
  }

  public toggleAdverts(value: boolean) {
    this.settings.settings.showAdverts = value;
    this.saveSettings(this.settings.settings);
    
    if (value) {
      Object.values((window as any).ads ?? {}).forEach((e: any) => e.refreshAd());
    } else {
      Object.values((window as any).ads ?? {}).forEach((e: any) => e.removeAd());
    }
  }

  public openProfile() {
    platform.get.utils.openUrl('https://minetogether.io/profile/edit');
  }

  public logout() {
    this.logoutAction();
    // get instances and store
    gobbleError(() => {
      sendMessage("ircQuitRequest", {})
    })

    platform.get.actions.logoutFromMinetogether();

    this.settings.settings.sessionString = undefined;
    this.saveSettings(this.settings.settings);

    this.$router.push('/');
  }
}
</script>
