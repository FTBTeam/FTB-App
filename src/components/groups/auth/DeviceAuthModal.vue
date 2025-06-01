<script lang="ts" setup>
import dayjs, {Dayjs} from 'dayjs';
import appPlatform from '@platform';
import { UiButton, Loader, Message, ProgressBar, Modal } from '@/components/ui';
import {
  CheckForCodeReturn,
  DeviceAuthResponse, DeviceCodeHolder,
  LoadCodeReturn, OnResultReturn,
} from '@/components/groups/auth/LoginTypes';
import {alertController} from '@/core/controllers/alertController';
import { onUnmounted, watch, ref } from 'vue';
import { safeLinkOpen } from '@/utils';
import { faExternalLink } from '@fortawesome/free-solid-svg-icons';

const {
  open,
  loadCode,
  checkForSuccess,
  onResult,
  requiresPolling = true,
  showQrCode = true,
  accountType,
  subtext,
} = defineProps<{
  open: boolean;
  loadCode: () => Promise<LoadCodeReturn>,
  checkForSuccess: (deviceCode: DeviceCodeHolder) => Promise<CheckForCodeReturn>;
  onResult: (data: any) => Promise<OnResultReturn>;
  requiresPolling?: boolean;
  showQrCode?: boolean;
  accountType: string;
  subtext: string;
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const deviceCodeDone = ref(false);  
const loggingIn = ref(false);
const logInError = ref("");

const loadingCode = ref(false);
const error = ref("");

const startedFlowAt = ref<Dayjs | null>(null);
const pollingInterval = ref(1000);
const deviceCodeData = ref<DeviceCodeHolder | null>(null);

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
  if (!deviceCodeData.value) {
    return;
  }
  
  await navigator.clipboard.writeText(deviceCodeData.value?.user_code);
  appPlatform.utils.openUrl(deviceCodeData.value?.verification_uri);
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
    const data = await loadCode();
    if ("error" in data) {
      error.value = data.error;
      return;
    }

    error.value = "";
    deviceCodeData.value = data;
    if (data.interval) {
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
  
  // This won't happen but we have to check for it anyway
  if (!deviceCodeData.value) {
    return;
  }

  pollLock.value = true;
  try {
    const data = await checkForSuccess(deviceCodeData.value);
    if ("pass" in data) {
      // We're just waiting for the user to login
      return;
    }
    
    if (!(data && "error" in data)) {
      // We can now use this data to jump through the hops required to get the user logged in
      continueTokenFlow(data.data).then(() => {})
      stopPolling();
    } else if ("error" in data) {
      error.value = data.error;
      stopPolling();
    } else {
      error.value = "Failed to get the token, a retry maybe resolve the issue.";
      stopPolling();
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

  if (requiresPolling) {
    pollRef.value = setInterval(async () => {
      await checkForToken();
    }, pollingInterval.value) as unknown as number;
  } else {
    checkForToken().catch(console.error)
  }
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
  deviceCodeData.value = null;
  pollLock.value = false;
  pollingInterval.value = 1000;
}

async function continueTokenFlow(data: any) {
  const typedData = data as DeviceAuthResponse;

  deviceCodeDone.value = true;
  loggingIn.value = true;
  try {
    const result = await onResult(typedData);
    if (result !== true && "error" in result) {
      alertController.error("Failed to login, please try again.");
      logInError.value = result.error;
      return;
    }
  } catch (e) {
    alertController.error("Failed to login, please try again.");
    logInError.value = "Failed to login, please try again.";
  } finally {
    loggingIn.value = false;
  }
}

function resetFinalStep() {
  loggingIn.value = false;
  logInError.value = "";
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
  if (!startedFlowAt.value || !deviceCodeData.value?.expires_in) {
    return 0;
  }

  // For example: If expires in is 900 seconds and we started 10 seconds ago, we should be at ~1.1%
  const end = startedFlowAt.value.add(deviceCodeData.value?.expires_in, "second");
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
  <Modal :open="open" :title="`Login with ${accountType}`" @closed="() => onClose()">
    <template v-if="!deviceCodeDone">
      <p class="mb-6">{{ subtext }}</p>

      <Loader v-if="loadingCode" />

      <Message type="danger" v-if="error">
        {{ error }}
      </Message>

      <div class="code font-bold text-4xl py-4 px-6 rounded-lg mb-4 text-center select-text" v-if="deviceCodeData?.user_code && !error">
        <span>{{ deviceCodeData?.user_code }}</span>
      </div>

      <div v-if="deviceCodeData?.expires_in && startedFlowAt" class="mb-6">
        <ProgressBar type="muted" :inverted="true" :do-progress-animation="false" :progress="remainingTime / 100" />
        
        <p class="text-muted mb-2 text-center mt-2">Token expires {{ startedFlowAt.add(deviceCodeData?.expires_in ?? 0, "second").fromNow() }}</p>
      </div>
      
      <UiButton :icon="faExternalLink" size="large" :full-width="true" v-if="deviceCodeData?.user_code && !error" @click="copyAndOpen" class="mt-2 w-full" type="primary">Copy and Open</UiButton>

      <hr class="mt-6 border-white border-opacity-25" v-if="deviceCodeData?.verification_uri" />
      
      <div class="flex items-center gap-6 mt-8" v-if="deviceCodeData?.verification_uri">
        <div class="bg-white rounded p-2" v-if="showQrCode">
          <img src="../../../assets/images/ms-link-qr-code.svg" alt="Microsoft Logo" width="100" />
        </div>
        <div class="flex-1">
          <p class="select-text block text-white text-opacity-75">If the button above does not work, try
            <a :href="deviceCodeData?.verification_uri" @click="safeLinkOpen" class="text-white text-opacity-100 hover:underline cursor-pointer">{{ deviceCodeData?.verification_uri }}</a> on any device.<span v-if="showQrCode"><br/><br/>Or try the QR code on your phone for quicker setup.</span></p>
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