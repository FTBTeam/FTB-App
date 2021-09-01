<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
    <div class="flex flex-col w-full lg:w-9/12 xl:w-8/12 mx-auto">
      <h1 class="text-2xl">Profile</h1>
      <div class="bg-sidebar-item p-5 rounded my-4">
        <div class="flex flex-col my-2 items-center">
          <img :src="`https://api.mymcuu.id/head/${avatarName}`" class="rounded-full" />
          <p class="text-lg mt-2">{{ auth.token.mc !== undefined ? auth.token.mc.display : auth.token.username }}</p>
          <p>
            {{
              auth.token.activePlan !== undefined && auth.token.activePlan !== null ? auth.token.activePlan.name : ''
            }}
          </p>

          <div class="mt-6 w-1/2">
            <ftb-button color="primary" class="text-center px-2 py-1" title="Coming Soon" @click="openSubscriptions"
              >Upgrade / Modify Subscription</ftb-button
            >
            <ftb-button color="primary" class="text-center px-2 py-1 my-2" title="Coming Soon" @click="openProfile"
              >Update Profile</ftb-button
            >
          </div>
          <div class="mt-6 w-1/2">
            <ftb-toggle
              label="Automatically open friends list"
              :value="settings.settings.autoOpenChat === true || settings.settings.autoOpenChat === 'true'"
              onColor="bg-primary"
              inline="true"
              @change="toggleAutoOpenChat"
            />
            <ftb-toggle
              label="Enable cloud save uploads "
              :value="settings.settings.cloudSaves === true || settings.settings.cloudSaves === 'true'"
              @change="toggleCloudSaves"
              :disabled="auth.token.activePlan === null"
              onColor="bg-primary"
              inline="true"
            />
            <ftb-toggle
              label="Enable Minetogether Connect"
              :value="settings.settings.mtConnect === true || settings.settings.mtConnect === 'true'"
              @change="toggleMTConnect"
              :disabled="auth.token.activePlan === null"
              onColor="bg-primary"
              inline="true"
            />
            <ftb-toggle
              label="Show adverts"
              :value="settings.settings.showAdverts === true || settings.settings.showAdverts === 'true'"
              @change="toggleAdverts"
              :disabled="auth.token.activePlan === null"
              onColor="bg-primary"
              inline="true"
            />
            <ftb-toggle
              label="Load Minecraft in FTBApp"
              :value="settings.settings.loadInApp === true || settings.settings.loadInApp === 'true'"
              @change="toggleLoading"
              :disabled="auth.token.activePlan === null"
              onColor="bg-primary"
              inline="true"
            />
          </div>
          <div class="mt-4 w-1/2">
            <ftb-button color="warning" class="text-center px-2 py-1 my-2" @click="logout">Logout</ftb-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import { Action, State } from 'vuex-class';
import { AuthState } from '../modules/auth/types';
import { SettingsState } from '../modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';

@Component({
  components: {
    'ftb-toggle': FTBToggle,
    PackCardWrapper,
  },
})
export default class ProfilePage extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('sendMessage') public sendMessage: any;
  @State('auth') private auth!: AuthState;
  @State('settings') private settings!: SettingsState;
  @Action('logout', { namespace: 'auth' }) private logoutAction!: () => void;

  get avatarName() {
    const provider = this.auth.token?.accounts.find(s => s.identityProvider === 'mcauth');
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

  public toggleMTConnect(value: boolean) {
    this.settings.settings.mtConnect = value;
    this.saveSettings(this.settings.settings);
  }

  public toggleAdverts(value: boolean) {
    this.settings.settings.showAdverts = value;
    this.saveSettings(this.settings.settings);
  }

  public toggleLoading(value: boolean) {
    this.settings.settings.loadInApp = value;
    this.saveSettings(this.settings.settings);
  }

  public openSubscriptions() {
    platform.get.utils.openUrl('https://ftb.plus/profile/subscriptions');
  }

  public openProfile() {
    platform.get.utils.openUrl('https://minetogether.io/profile/edit');
  }

  public logout() {
    this.logoutAction();
    // get instances and store
    this.sendMessage({
      payload: {
        type: 'ircQuitRequest',
      },
    });

    platform.get.actions.logoutFromMinetogether();

    this.settings.settings.sessionString = undefined;
    this.saveSettings(this.settings.settings);

    this.$router.push('/');
  }
}
</script>
