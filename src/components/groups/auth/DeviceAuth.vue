<script setup lang="ts">
import {onMounted, onUnmounted, ref} from "vue";
import QrCode from "@/components/ui/QrCode.vue";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import {AccountProvider} from "@/core/types/javaApi";
import {Loader, Message, UiButton} from "@/components/ui";
import dayjs, {Dayjs} from "dayjs";
import {faCircleNotch, faCopy, faExternalLink} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import appPlatform from '@platform'
import {useCopyToClipboard} from "@/composables/useCopyToClipboard.ts";

const {
  provider
} = defineProps<{
  provider: AccountProvider
}>()

const emit = defineEmits<{
  (event: 'started', flowId: string): void;
  (event: 'completed'): void;
}>()

const loading = ref(true)
const error = ref<string | null>(null)

const {copied, copy} = useCopyToClipboard();

const redirectUri = ref("")
const expiresAt = ref<Dayjs | null>(null);
const timerRef = ref<number | null>(null);
const interval = ref(5); // Seconds between polls, set by the backend
const lastPollAt = ref<number>(-1);
const deviceCode = ref("");
const responseLock = ref(false);
const flowId = ref<string | null>(null);
const timeLeft = ref("")

let completed = false;
let unmounted = false;

onMounted(() => {
  startFlow();
})

async function startFlow() {
  loading.value = true;
  try {
    clearPoll()
    
    const result = await sendMessage("accounts.device.flow", {
      provider: provider,
      flowStage: "GET_CODE"
    })
    
    if (unmounted) {
      if (result.flowId) {
        cancel(result.flowId);
      }
      return;
    }
    
    if (result.status !== "CODE_ISSUED" || !result.code || !result.flowId) {
      error.value = result.reason ?? "Failed to get a login code. Please try again.";
      return;
    }
    
    flowId.value = result.flowId;
    deviceCode.value = result.code.userCode
    expiresAt.value = dayjs.unix(result.code.expiresAt)
    redirectUri.value = result.code.verificationUriComplete || result.code.verificationUri
    interval.value = result.interval || 5;
    emit('started', result.flowId);
    
    timerRef.value = setInterval(() => {
      poll().catch(e => console.log(e))
    }, 1000) as any as number // Once a second!
  } catch (e) {
    console.error(e);
    error.value = "Failed to get a login code. Please try again.";
  } finally {
    loading.value = false;
  }
}

async function poll() {
  timeLeft.value = expiresAt.value?.fromNow() ?? ""
  
  const now = Math.round(Date.now() / 1000);
  const diff = now - lastPollAt.value;
  if (diff < interval.value || responseLock.value || !flowId.value) {
    return;
  }

  responseLock.value = true;
  lastPollAt.value = now;
  try {
    const result = await sendMessage("accounts.device.flow", {
      provider: provider,
      flowStage: "CHECK_FOR_TOKEN",
      flowId: flowId.value
    })
    
    if (unmounted) {
      return;
    }

    switch (result.status) {
      case "PENDING":
        interval.value = result.interval || interval.value;
        break;
      case "COMPLETE":
        completed = true;
        clearPoll();
        emit('completed');
        break;
      case "FAILED":
        error.value = result.reason ?? "Unknown error. Please report this to our team."
        clearPoll();
        clearState();
        break;
      case "CANCELLED":
        clearPoll();
        break;
    }
  } finally {
    responseLock.value = false;
  }
}

onUnmounted(() => {
  unmounted = true;
  clearPoll()
  if (flowId.value && !completed) {
    cancel(flowId.value);
  }
})

function cancel(id: string) {
  sendMessage("accounts.device.flow", {
    provider: provider,
    flowStage: "CANCEL",
    flowId: id
  }).catch((e) => console.error(e))
}

function clearPoll() {
  if (timerRef.value !== null) {
    clearInterval(timerRef.value)
    timerRef.value = null;
  }
}

function clearState() {
  redirectUri.value = "";
  expiresAt.value = null
  interval.value = 5;
  lastPollAt.value = -1;
  deviceCode.value = ""
  flowId.value = null
  timeLeft.value = ""
}
</script>

<template>
  <div v-if="!loading && !error">
    <div class="flex items-start mb-6 gap-6">
      <div>
        <QrCode :input="redirectUri" />
        <p class="text-white/80 text-sm mt-2">Scan to use your phone</p>
      </div>

      <div class="flex-1">
        <p class="text-white/80 mb-2">Your code</p>
        <div class="flex gap-2 justify-stretch mb-4">
          <div class="bg-black/30 code font-mono font-bold text-4xl py-4 px-6 rounded-lg text-center select-text w-full" v-if="deviceCode">
            <span>{{ deviceCode }}</span>
          </div>
          
          <UiButton class="flex! items-center" :icon="faCopy" :aria-label="copied ? 'Copied!' : 'Copy to clipboard'" @click="copy(redirectUri)"></UiButton>
        </div>
        <UiButton :icon="faExternalLink" size="large" fullWidth type="primary" @click="appPlatform.utils.openUrl(redirectUri)">
          Copy & Open
        </UiButton>
        <p class="text-white/80 mt-3">Paste the code at <a class="text-blue-400 hover:text-blue-500 underline" :href="redirectUri">{{ provider === 'MICROSOFT' ? 'microsoft.com/link' : 'identity.feed-the-beast.com' }}</a>, sign in, then come back here.</p>
      </div>
    </div>

    <div class="bg-black/30 rounded-lg p-4 flex gap-4 items-center">
      <FontAwesomeIcon :icon="faCircleNotch" spin fixedWidth />
      <p class="flex-1">Waiting for you to enter the code...</p>
      <p class="text-white/80">{{ timeLeft }}</p>
    </div>
  </div>
  
  <Message type="danger" title="Failed to continue" v-if="error && !loading">
    {{error}}
  </Message>
  
  <Loader v-if="loading" />
</template>