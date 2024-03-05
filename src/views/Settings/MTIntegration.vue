<template>
  <div class="minetogether-profile">
    <div class="avatar sm:flex items-center mb-16">
      <div class="account-info flex items-center">
        <img alt="Profile avatar" :src="getMinecraftHead(avatarName)" width="80" height="80" class="rounded-full mr-6" />
        <div class="name">
          <p class="text-lg bold">{{ getMtProfile?.display ? getMtProfile.display : getMtAccount?.username }}</p>
          <p class="opacity-75">
            {{
              getMtAccount?.activePlan !== undefined && getMtAccount?.activePlan !== null
                ? getMtAccount.activePlan.name
                : 'No paid plan'
            }}
          </p>
        </div>
      </div>
      <div class="ml-auto mt-10 sm:mt-0 flex items-center gap-2">
        <ui-button type="success" icon="external-link-square-alt" @click="openProfile">Update account</ui-button>
        <ui-button type="warning" icon="sign-out" @click="logout" :working="working">Logout</ui-button>
      </div>
    </div>

    <div class="">      
      <ui-toggle
        label="Show adverts"
        desc="Any paid plan can optionally disable ads throughout the app"
        :value="settings.settings.appearance.showAds"
        @input="toggleAdverts"
        :disabled="!getMtAccount?.activePlan"
      />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import {ns, nsAction} from '@/core/state/appState';
import {MineTogetherAccount, MineTogetherProfile} from '@/core/@types/javaApi';
import store from '@/modules/store';
import {InstanceActions} from '@/core/actions/instanceActions';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';

@Component({
  methods: {getMinecraftHead},
  components: {
    UiToggle,
    UiButton,
  },
})
export default class MTIntegration extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('settings') private settings!: SettingsState;

  @Getter("profile", ns("v2/mtauth")) getMtProfile!: MineTogetherProfile | null;
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;
  
  working = false;

  get avatarName() {
    return this.getMtAccount?.accounts?.find((s) => s.identityProvider === 'mcauth')?.userId
  }

  public toggleAdverts(value: boolean) {
    this.settings.settings.appearance.showAds = value;
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

  public async logout() {
    this.working = true;
    await MTIntegration.logoutFromMineTogether();
    this.working = false;

    await this.$router.push('/');
  }
  
  public static async logoutFromMineTogether() {
    const res = await sendMessage("minetogetherLogoutHandler", {});
    if (!res || !res.success) {
      alertController.error(`Failed to logout from MineTogether...`)
      return;
    }
    
    await store.dispatch(nsAction("v2/mtauth", 'setProfile'), null, {root: true});
    await store.dispatch(nsAction("v2/mtauth", 'setAccount'), null, {root: true});
    
    if (!store.state["v2/apiCredentials"].wasUserSet) {
      await store.dispatch(nsAction("v2/apiCredentials", 'resetCredentials'), null, {root: true});
    }
    
    await InstanceActions.clearInstanceCache(false);
  }
}
</script>
