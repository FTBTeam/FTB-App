<script lang="ts" setup>
import dayjs, {Dayjs} from 'dayjs';
import appPlatform from '@platform';
import { UiButton, Loader, Message, ProgressBar, Modal } from '@/components/ui';
import {DeviceAuthResponse} from '@/components/groups/auth/LoginTypes';
import {JavaFetch} from '@/core/javaFetch';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import { onUnmounted, watch, ref } from 'vue';
import { safeLinkOpen } from '@/utils';
import { useAccountsStore } from '@/store/accountsStore.ts';

const accountStore = useAccountsStore();

const {
  open
} = defineProps<{
  open: boolean;
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const deviceCodeDone = ref(false);  
const loggingIn = ref(false);
const logInError = ref("");
const logInErrorCode = ref("");

const loadingCode = ref(false);
const error = ref("");

const startedFlowAt = ref<Dayjs | null>(null);
const userCode = ref("");
const deviceCode = ref("");
const verificationUri = ref("");
const expiresIn = ref(0);
const pollingInterval = ref(1000);

const pollRef = ref< number | null>(null);
const pollLock = ref(false);
const timerRef = ref<number | null>(null);

const remainingTime = ref(0);

watch(() => open, (newValue) => {
  if (newValue) {
    loadInitialCode();
  } else {
    stopPolling();
  }
})

onUnmounted(() => {
  stopPolling();
})

async function copyAndOpen() {
  await navigator.clipboard.writeText(userCode.value);
  appPlatform.utils.openUrl(verificationUri.value);
}

async function retryInitialCode() {
  error.value = "";
  resetFinalStep()

  // Just in case we're polling
  stopPolling();
  await loadInitialCode();
}

/**
 * Makes the first step in the device flow to get the code that the user will then provide to the login page
 */
async function loadInitialCode() {
  loadingCode.value = true;

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
      error.value = "Failed to get the code, a retry maybe resolve the issue.";
      return;
    }

    const data = await req.json();
    // Very verbose checking to ensure we have all the required data
    if (!("user_code" in data) || !("device_code" in data) || !("verification_uri" in data) || !("expires_in" in data)) {
      error.value = "Failed to get the code, a retry maybe resolve the issue.";
      return;
    }

    error.value = "";
    userCode.value = data.user_code;
    deviceCode.value = data.device_code;
    verificationUri.value = data.verification_uri;
    expiresIn.value = data.expires_in;

    if ("interval" in data) {
      // This is returned in seconds, we need it in milliseconds
      pollingInterval.value = data.interval * 1000;
    }

    // Start polling for the token
    pollForToken();
  } catch (e) {
    error.value = "Failed to get the code, a retry maybe resolve the issue.";
    console.error(e);
  } finally {
    loadingCode.value = false;
  }
}

async function checkForToken() {
  if (pollLock.value) {
    return;
  }

  pollLock.value = true;
  try {
    const req: any = await JavaFetch.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
      .method("POST")
      .contentType("application/x-www-form-urlencoded")
      .body(new URLSearchParams({
        client_id: "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8",
        grant_type: "urn:ietf:params:oauth:grant-type:device_code",
        device_code: deviceCode.value
      }).toString())
      .execute();

    // We're expecting this error
    if (req?.statusCode === 400) {
      const data = await req.json();
      if (!("error" in data)) {
        error.value = "Failed to get the token, a retry maybe resolve the issue.";
        return;
      }

      // We're just waiting for the user to login
      if (data.error === "authorization_pending") {
        return;
      }

      if (data.error === "expired_token") {
        error.value = "The token has expired, please try again.";
        stopPolling();
        return;
      }

      if (data.error === "authorization_declined") {
        stopPolling();
        error.value = "You denied the login request, please try again or close the window.";
        return;
      }

      if (data.error === "bad_verification_code") {
        error.value = "Something has gone wrong, please try again.";
        stopPolling();
        return;
      }

      if (data.error === "invalid_scope") {
        // TODO: Support links
        error.value = "It looks like the FTB Apps client hasn't been setup correctly, please create an issue on our Github (https://github.com/FTBTeam/FTB-App/issues)";
        stopPolling();
        return;
      }
    } else {
      const data = await req.json();

      if ("token_type" in data && "access_token" in data && "refresh_token" in data && "expires_in" in data) {
        // We can now use this data to jump through the hops required to get the user logged in
        continueTokenFlow(data).then(() => {})
        stopPolling();
      } else {
        error.value = "Failed to get the token, a retry maybe resolve the issue.";
        stopPolling();
      }
    }
  } catch (e) {
    console.error(e);
  } finally {
    pollLock.value = false;
  }
}

function pollForToken() {
  if (pollRef.value !== null) {
    clearInterval(pollRef.value);
  }

  startedFlowAt.value = dayjs();

  // In charge of updating the UI to reflect the time data
  timerRef.value = setInterval(() => {
    remainingTime.value = expiresInAsPercentage();
  }, 1000) as unknown as number; // Update every second

  pollRef.value = setInterval(async () => {
    await checkForToken();
  }, pollingInterval.value) as unknown as number;
}

function stopPolling() {
  if (pollRef.value) {
    clearInterval(pollRef.value);
  }

  if (timerRef.value) {
    clearInterval(timerRef.value);
  }

  timerRef.value = null;
  pollRef.value = null;
  startedFlowAt.value = null;
  deviceCode.value = "";
  userCode.value = "";
  verificationUri.value = "";
  expiresIn.value = 0;
  pollLock.value = false;
  pollingInterval.value = 1000;
}

async function continueTokenFlow(data: any) {
  const typedData = data as DeviceAuthResponse;

  deviceCodeDone.value = true;
  loggingIn.value = true;
  try {
    const res = await sendMessage("profiles.ms.authenticate", {
      liveExpires: typedData.expires_in as any,
      liveAccessToken: typedData.access_token as any,
      liveRefreshToken: typedData.refresh_token as any,
    })

    loggingIn.value = false;
    if (!res.success) {
      logInError.value = res.message;
      logInErrorCode.value = res.code;
      return;
    }

    await accountStore.loadProfiles();
  } catch (e) {
    alertController.error("Failed to login, please try again.");
    logInError.value = "Failed to login, please try again.";
    logInErrorCode.value = "unknown";
  } finally {
    loggingIn.value = false;
  }
}

function resetFinalStep() {
  loggingIn.value = false;
  logInError.value = "";
  logInErrorCode.value = "";
  deviceCodeDone.value = false;
}

function onClose() {
  stopPolling();
  resetFinalStep();
  emit("closed");
}

function expiresInAsPercentage() {
  // Take the started time and add the expires in time to get the end time
  // Then figure out a 0 - 100% value based on the current time
  if (!startedFlowAt.value || !expiresIn.value) {
    return 0;
  }

  // For example: If expires in is 900 seconds and we started 10 seconds ago, we should be at ~1.1%
  const end = startedFlowAt.value.add(expiresIn.value, "second");
  const now = dayjs();

  // If we're past the end time, we're at 100%
  if (now.isAfter(end)) {
    return 100;
  }

  // Otherwise, figure out the percentage
  const diff = end.diff(startedFlowAt.value, "second");
  const current = now.diff(startedFlowAt.value, "second");

  return (current / diff) * 100;
}
</script>

<template>
  <Modal :open="open" title="Login with Microsoft" @closed="() => onClose()">
    <template v-if="!deviceCodeDone">
      <p class="mb-6">Login to Minecraft via your Microsoft account. Clicking the button below will open the Microsoft Login page and copy the code to your clipboard.</p>

      <Loader v-if="loadingCode" />

      <Message type="danger" v-if="error">
        {{ error }}
      </Message>

      <div class="code font-bold text-4xl py-4 px-6 rounded-lg mb-4 text-center select-text" v-if="userCode && !error">
        <span>{{ userCode }}</span>
      </div>

      <div v-if="expiresIn && startedFlowAt" class="mb-6">
        <ProgressBar type="muted" :inverted="true" :do-progress-animation="false" :progress="remainingTime / 100" />
        
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
            <a :href="verificationUri" @click="safeLinkOpen" class="text-white text-opacity-100 hover:underline cursor-pointer">{{ verificationUri }}</a> on any device.<br/><br/>Or try the QR code on your phone for quicker setup.</p>
        </div>
      </div>

      <UiButton type="success" :full-width="true" v-if="error" @click="retryInitialCode" class="mt-6">Retry</UiButton>
    </template>
    
    <template v-else>
      <Loader title="Logging in ..."  v-if="loggingIn" />
      
      <template v-if="!loggingIn && logInError">
        <Message type="danger" class="select-text">
          <b class="mb-2 block">Something has gone wrong!</b>
          <p>{{ logInError }}</p>
          
          <div class="wysiwyg mt-6">
            <span class="opacity-75 block mb-1">Reference</span>
            <code class="inline-block">{{ logInErrorCode }}</code>
          </div>
        </Message>

        <UiButton type="success" :full-width="true" @click="retryInitialCode" class="mt-6">Retry</UiButton>
      </template>
      
      <p v-if="!loggingIn && !logInError">
        You have successfully logged in, you can now close this window.
      </p>
    </template>
  </Modal>
</template>

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