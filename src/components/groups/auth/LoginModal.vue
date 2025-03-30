<template>
  <modal :open="open" title="Login with Microsoft" @closed="() => onClose()">
    <template v-if="!deviceCodeDone">
      <p class="mb-6">Login to Minecraft via your Microsoft account. Clicking the button below will open the Microsoft Login page and copy the code to your clipboard.</p>

      <Loader v-if="loadingCode" />

      <message type="danger" v-if="error">
        {{ error }}
      </message>

      <div class="code font-bold text-4xl py-4 px-6 rounded-lg mb-4 text-center select-text" v-if="userCode && !error">
        <span>{{ userCode }}</span>
      </div>

      <div v-if="expiresIn && startedFlowAt" class="mb-6">
        <progress-bar type="muted" :inverted="true" :do-progress-animation="false" :progress="remainingTime / 100" />
        
        <p class="text-muted mb-2 text-center mt-2">Token expires {{ startedFlowAt.add(expiresIn, "second").fromNow() }}</p>
      </div>
      
      <UiButton icon="external-link" size="large" :full-width="true" v-if="userCode && !error" @click="copyAndOpen" class="mt-2 w-full" type="primary">Copy and Open</UiButton>

      <hr class="mt-6 border-white border-opacity-25" v-if="verificationUri" />
      
      <div class="flex items-center gap-6 mt-8" v-if="verificationUri">
        <div class="bg-white rounded p-2">
          <img src="../../../assets/images/ms-link-qr-code.svg" alt="Microsoft Logo" width="100" />
        </div>
        <div class="flex-1">
          <p class="select-text block text-white text-opacity-75">If the button above does not work, try
            <a :href="verificationUri" @click="openExternal" class="text-white text-opacity-100 hover:underline cursor-pointer">{{ verificationUri }}</a> on any device.<br/><br/>Or try the QR code on your phone for quicker setup.</p>
        </div>
      </div>

      <UiButton type="success" :full-width="true" v-if="error" @click="retryInitialCode" class="mt-6">Retry</UiButton>
    </template>
    
    <template v-else>
      <Loader title="Logging in ..."  v-if="loggingIn" />
      
      <template v-if="!loggingIn && logInError">
        <message type="danger" class="select-text">
          <b class="mb-2 block">Something has gone wrong!</b>
          <p>{{ logInError }}</p>
          
          <div class="wysiwyg mt-6">
            <span class="opacity-75 block mb-1">Reference</span>
            <code class="inline-block">{{ logInErrorCode }}</code>
          </div>
        </message>

        <UiButton type="success" :full-width="true" @click="retryInitialCode" class="mt-6">Retry</UiButton>
      </template>
      
      <p v-if="!loggingIn && !logInError">
        You have successfully logged in, you can now close this window.
      </p>
    </template>
  </modal>
</template>

<script lang="ts">
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import dayjs, {Dayjs} from 'dayjs';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import platform from '@/utils/interface/electron-overwolf';
import UiButton from '@/components/ui/UiButton.vue';
import {DeviceAuthResponse} from '@/components/groups/auth/LoginTypes';
import Loader from '@/components/ui/Loader.vue';
import {JavaFetch} from '@/core/javaFetch';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {Action} from 'vuex-class';

@Component({
  components: {Loader, UiButton, ProgressBar}
})
export default class LoginModal extends Vue {
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles!: () => Promise<void>;

  @Prop() open!: boolean;
  
  deviceCodeDone = false;
  loggingIn = false;
  logInError = "";
  logInErrorCode = "";
  
  loadingCode = false;
  error = "";
  
  startedFlowAt: Dayjs | null = null;
  userCode = "";
  deviceCode = "";
  verificationUri = "";
  expiresIn = 0;
  pollingInterval = 1000;
  
  pollRef: number | null = null;
  pollLock = false;
  
  timerRef: number | null = null;
  remainingTime = 0;
  
  @Watch("open")
  onOpenChange() {
    if (this.open) {
      this.loadInitialCode();
    } else {
      this.stopPolling();
    }
  }
  
  destroyed() {
    this.stopPolling();
  }

  async copyAndOpen() {
    await navigator.clipboard.writeText(this.userCode);
    platform.get.utils.openUrl(this.verificationUri);
  }
  
  async retryInitialCode() {
    this.error = "";
    this.resetFinalStep()
    
    // Just in case we're polling
    this.stopPolling();
    await this.loadInitialCode();
  }
  
  /**
   * Makes the first step in the device flow to get the code that the user will then provide to the login page
   */
  private async loadInitialCode() {
    this.loadingCode = true;
    
    try {
      const req: any = await JavaFetch.create(`https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode`)
        .method("POST")
        .contentType("application/x-www-form-urlencoded")
        .body(new URLSearchParams({
          client_id: "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8",
          scope: "offline_access XboxLive.signin" // Same as our own auth service
        }).toString())
        .execute()

      if (!req?.statusCode || req.statusCode !== 200) {
        this.error = "Failed to get the code, a retry maybe resolve the issue.";
        return;
      }
      
      const data = await req.json();
      // Very verbose checking to ensure we have all the required data
      if (!("user_code" in data) || !("device_code" in data) || !("verification_uri" in data) || !("expires_in" in data)) {
        this.error = "Failed to get the code, a retry maybe resolve the issue.";
        return;
      }
      
      this.error = "";
      this.userCode = data.user_code;
      this.deviceCode = data.device_code;
      this.verificationUri = data.verification_uri;
      this.expiresIn = data.expires_in;
      
      if ("interval" in data) {
        // This is returned in seconds, we need it in milliseconds
        this.pollingInterval = data.interval * 1000;
      }
      
      // Start polling for the token
      this.pollForToken();
    } catch (e) {
      this.error = "Failed to get the code, a retry maybe resolve the issue.";
      console.error(e);
    } finally {
      this.loadingCode = false;
    }
  }
  
  private async checkForToken() {
    if (this.pollLock) {
      return;
    }
    
    this.pollLock = true;
    try {
      const req: any = await JavaFetch.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .method("POST")
        .contentType("application/x-www-form-urlencoded")
        .body(new URLSearchParams({
          client_id: "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8",
          grant_type: "urn:ietf:params:oauth:grant-type:device_code",
          device_code: this.deviceCode
        }).toString())
        .execute();
      
      // We're expecting this error
      if (req?.statusCode === 400) {
        const data = await req.json();
        if (!("error" in data)) {
          this.error = "Failed to get the token, a retry maybe resolve the issue.";
          return;
        }
        
        // We're just waiting for the user to login
        if (data.error === "authorization_pending") {
          return;
        }
        
        if (data.error === "expired_token") {
          this.error = "The token has expired, please try again.";
          this.stopPolling();
          return;
        }
        
        if (data.error === "authorization_declined") {
          this.stopPolling();
          this.error = "You denied the login request, please try again or close the window.";
          return;
        }
        
        if (data.error === "bad_verification_code") {
          this.error = "Something has gone wrong, please try again.";
          this.stopPolling();
          return;
        }
        
        if (data.error === "invalid_scope") {
          // TODO: Support links
          this.error = "It looks like the FTB Apps client hasn't been setup correctly, please create an issue on our Github (https://github.com/FTBTeam/FTB-App/issues)";
          this.stopPolling();
          return;
        }
      } else {
        const data = await req.json();
        
        if ("token_type" in data && "access_token" in data && "refresh_token" in data && "expires_in" in data) {
          // We can now use this data to jump through the hops required to get the user logged in
          this.continueTokenFlow(data).then(() => {})
          this.stopPolling();
        } else {
          this.error = "Failed to get the token, a retry maybe resolve the issue.";
          this.stopPolling();
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      this.pollLock = false;
    }
  }
  
  private pollForToken() {
    if (this.pollRef !== null) {
      clearInterval(this.pollRef);
    }
    
    this.startedFlowAt = dayjs();
    
    // In charge of updating the UI to reflect the time data
    this.timerRef = setInterval(() => {
      this.remainingTime = this.expiresInAsPercentage();
    }, 1000) as unknown as number; // Update every second
    
    this.pollRef = setInterval(async () => {
      await this.checkForToken();
    }, this.pollingInterval) as unknown as number;
  }
  
  private stopPolling() {
    if (this.pollRef) {
      clearInterval(this.pollRef);
    }
    
    if (this.timerRef) {
      clearInterval(this.timerRef);
    }
    
    this.timerRef = null;
    this.pollRef = null;
    this.startedFlowAt = null;
    this.deviceCode = "";
    this.userCode = "";
    this.verificationUri = "";
    this.expiresIn = 0;
    this.pollLock = false;
    this.pollingInterval = 1000;
  }

  private async continueTokenFlow(data: any) {
    const typedData = data as DeviceAuthResponse;
    
    this.deviceCodeDone = true;
    this.loggingIn = true;
    try {
      const res = await sendMessage("profiles.ms.authenticate", {
        liveExpires: typedData.expires_in as any,
        liveAccessToken: typedData.access_token as any,
        liveRefreshToken: typedData.refresh_token as any,
      })
      
      this.loggingIn = false;
      if (!res.success) {
        this.logInError = res.message;
        this.logInErrorCode = res.code;
        return;
      }
      
      await this.loadProfiles();
    } catch (e) {
      alertController.error("Failed to login, please try again.");
      this.logInError = "Failed to login, please try again.";
      this.logInErrorCode = "unknown";
    } finally {
      this.loggingIn = false;
    }
  }
  
  private resetFinalStep() {
    this.loggingIn = false;
    this.logInError = "";
    this.logInErrorCode = "";
    this.deviceCodeDone = false;
  }
  
  onClose() {
    this.stopPolling();
    this.resetFinalStep();
    this.$emit("closed");
  }
  
  private expiresInAsPercentage() {
    // Take the started time and add the expires in time to get the end time
    // Then figure out a 0 - 100% value based on the current time
    if (!this.startedFlowAt || !this.expiresIn) {
      return 0;
    }
    
    // For example: If expires in is 900 seconds and we started 10 seconds ago, we should be at ~1.1%
    const end = this.startedFlowAt.add(this.expiresIn, "second");
    const now = dayjs();
    
    // If we're past the end time, we're at 100%
    if (now.isAfter(end)) {
      return 100;
    }
    
    // Otherwise, figure out the percentage
    const diff = end.diff(this.startedFlowAt, "second");
    const current = now.diff(this.startedFlowAt, "second");
    
    return (current / diff) * 100;
  }
}
</script>

<style lang="scss" scoped>
.code {
  background-color: #454545;
  border: 2px solid #1C1C1C;
  letter-spacing: .5em;
  font-family: 'JetBrains Mono', monospace;
  
  span {
    padding-left: 1.5rem;
  }
}
</style>