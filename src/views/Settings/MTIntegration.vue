<template>
  <div class="minetogether-profile">
    <div class="avatar sm:flex items-center mb-16">
      <div class="account-info flex items-center">
        <img alt="Profile avatar" :src="getMinecraftHead(avatarName)" width="80" height="80" class="rounded-full mr-6" />
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
      <div class="ml-auto mt-10 sm:mt-0 flex items-center gap-2">
        <ui-button type="success" icon="external-link-square-alt" @click="openProfile">Update account</ui-button>
        <ui-button type="warning" icon="sign-out" @click="logout">Logout</ui-button>
      </div>
    </div>

    <div class="">      
      <ui-toggle
        label="Show adverts"
        desc="Any paid plan can optionally disable ads throughout the app"
        :value="settings.settings.showAdverts === true || settings.settings.showAdverts === 'true'"
        @input="toggleAdverts"
        :disabled="!auth?.token?.activePlan"
      />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';
import UiToggle from '@/components/core/ui/UiToggle.vue';

@Component({
  methods: {getMinecraftHead},
  components: {
    UiToggle,
    UiButton,
  },
})
export default class MTIntegration extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('auth') private auth!: AuthState;
  @State('settings') private settings!: SettingsState;
  @Action('logout', { namespace: 'auth' }) private logoutAction!: () => void;

  get avatarName() {
    return this.auth.token?.accounts.find((s) => s.identityProvider === 'mcauth')?.userId
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
    
    this.settings.settings.sessionString = undefined;
    this.saveSettings(this.settings.settings);

    this.$router.push('/');
  }
}
</script>
