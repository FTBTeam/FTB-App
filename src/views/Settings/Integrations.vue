<template>
  <div class="app-integrations">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-2">Integrations</h1>
      <p class="text-muted">Integrate with MineTogether to use cloudsaves and private packs.</p>
    </div>
    
    <div class="active-integrations integrations mb-8">
      <p v-if="!getMtAccount">
        Looks like you've not got any integrations setup, use the 'available integrations' section below to get started.
      </p>

      <div class="card" v-else>
        <div class="logo"><img src="@/assets/images/mt-logo.webp" alt="" /></div>
        <div class="main" v-if="getMtAccount">
          <div class="name font-bold mb-2 flex items-center">
            <div class="avatar mr-2">
              <img
                :src="getMinecraftHead(avatarName)"
                style="margin-right: 0.75em; width: 40px; height: 40px"
                class="rounded-full"
              />
            </div>
            <div class="name flex flex-col">
              <span class="block">{{getMtProfile?.display?.split("#")[0] || getMtAccount.username || "Unknown" }}</span>
              <span
                v-if="getMtProfile?.display"
                class="text-xs opacity-50 hash"
              >
                #{{ getMtProfile?.display.split('#')[1] }}
              </span>
            </div>
          </div>
          <div class="setup inline-block mt-4 text-sm">
            <ui-button :wider="true" type="success" @click="logout" :working="working" icon="sign-out">Logout</ui-button>
          </div>
        </div>
        <div class="main flex" v-else>
          <font-awesome-icon icon="spinner" spin class="mr-3" />
          <span>Loading....</span>
        </div>
      </div>
    </div>

    <h3 class="text-xl font-bold mb-4" v-if="!getMtAccount">Available integrations</h3>
    <div class="integrations" v-if="!getMtProfile">
      <div class="card">
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
import {Action, Getter} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';
import {alertController} from '@/core/controllers/alertController';
import {ns} from '@/core/state/appState';
import {MineTogetherAccount, MineTogetherProfile} from '@/core/@types/javaApi';
import MTIntegration from '@/views/Settings/MTIntegration.vue';
import {createLogger} from '@/core/logger';
import {SetAccountMethod, SetProfileMethod} from '@/core/state/core/mtAuthState';
import {StoreCredentialsAction} from '@/core/state/core/apiCredentialsState';
import {InstanceActions} from '@/core/actions/instanceActions';
import {emitter} from '@/utils';

@Component({
  components: {UiButton},
  methods: {getMinecraftHead}
})
export default class Integrations extends Vue {
  private logger = createLogger("Integrations.vue");

  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;

  @Getter("profile", ns("v2/mtauth")) getMtProfile!: MineTogetherProfile | null;
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;

  @Action("setProfile", ns("v2/mtauth")) setProfile!: SetProfileMethod;
  @Action("setAccount", ns("v2/mtauth")) setAccount!: SetAccountMethod;
  @Action("storeCredentials", ns("v2/apiCredentials")) storeCredentials!: StoreCredentialsAction;
  @Getter("wasUserSet", ns("v2/apiCredentials")) wasUserSet!: boolean;

  working = false;
  
  mounted() {
    emitter.on('mt:auth-callback', this.login);
  }
  
  destroyed() {
    emitter.off('mt:auth-callback', this.login);
  }
  
  get avatarName() {
    return this.getMtAccount?.accounts?.find((s) => s.identityProvider === 'mcauth')?.userId
  }
  
  async login(callbackData: any) {
    this.working = true
    try {
      const token = callbackData.token;
      const appToken = callbackData.appToken;

      if (!token || !appToken) {
        alertController.error("Failed to login to MineTogether due to missing data");
        return;
      }

      const result = await sendMessage("minetogetherAuthentication", {
        authType: "login",
        apiKey: token,
        appToken: appToken
      })

      if (!result || !result.success) {
        alertController.error(`Failed to login to MineTogether due to ${result?.message || "Unknown error"}`)
        return;
      }

      // We have to handle the profile being null maybe
      if (!result.basicData) {
        alertController.error(`Failed to login to MineTogether due to as we could not find your profile`)
        return;
      }

      // We're good, the backend has stored what we need, we'll just update our data stores
      const {basicData, profile} = result;
      if (basicData) {
        this.logger.info("Setting account");
        await this.setAccount(basicData.account);

        if (!this.wasUserSet) {
          if (basicData.data.modpacksToken) {
            this.logger.info("Setting modpacks token");
            await this.storeCredentials({
              apiSecret: basicData.data.modpacksToken,
            });
          }
        }
      }

      if (profile) {
        this.logger.info("Setting profile");
        await this.setProfile(profile);
      }

      await InstanceActions.clearInstanceCache(false);
    } catch (e: any) {
      this.logger.error("Failed to login to MineTogether due to an error", e);
      alertController.error(`Failed to login to MineTogether due to an error: ${e.message || "Unknown error"}`)
    } finally {
      this.working = false;
    }
  }

  openLogin() {
    platform.get.utils.openUrl("https://minetogether.io/api/login?redirect=ftb://mauth/process")
  }

  public async logout() {
    this.working = true;
    await MTIntegration.logoutFromMineTogether();
    this.working = false;

    await this.$router.push('/');
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
