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

const {
  provider
} = defineProps<{
  provider: AccountProvider
}>()

const loading = ref(true)
const error = ref<string | null>(null)

const redirectUri = ref("")
const polling = ref(false);
const expiresIn = ref<Dayjs | null>(null);
const timerRef = ref<number | null>(null);
const interval = ref(5); // Interval in seconds
const lastPollAt = ref<number>(-1);
const deviceCode = ref("");
const responseLock = ref(false);
const deviceToken = ref("");
const timeLeft = ref("")

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
    
    if (result.reason || !result.start) {
      error.value = result.reason || "Failed to get reply from Microsoft";
      return;
    }
    
    polling.value = true;
    deviceCode.value = result.start.user_code
    deviceToken.value = result.start.device_code;
    expiresIn.value = dayjs().add(result.start.expires_in, 'seconds')
    redirectUri.value = result.start.verification_uri_complete || result.start.verification_uri
    interval.value = result.start.interval ?? 5;
    
    timerRef.value = setInterval(() => {
      poll().catch(e => console.log(e))
    }, 1000) as any as number // Once a second!
  } finally {
    loading.value = false;
  }
}

async function poll() {
  timeLeft.value = expiresIn.value?.fromNow() ?? ""
  
  const now = Math.round(Date.now() / 1000);
  const diff = now - lastPollAt.value;
  if (diff >= interval.value) {
    if (responseLock.value) {
      return;
    }

    responseLock.value = true;
    lastPollAt.value = now;
    // Poll!
    try {
      console.log("Polling")
      const result = await sendMessage("accounts.device.flow", {
        provider: provider,
        flowStage: "CHECK_FOR_TOKEN",
        deviceToken: deviceToken.value
      })

      if (!result.success) {
        error.value = result.reason ?? "Unknown error. Please report this to our team."
        clearPoll();
        clearState();
        return;
      }
      
      if (result.slowDown) {
        interval.value += 5; // Add 5 seconds to the polling interval
        return;
      }
      
      if (result.isPending) {
        console.log("Pending...")
      }
    } finally {
      responseLock.value = false;
    }
  }
}

onUnmounted(() => {
  clearPoll()
  sendMessage("accounts.device.flow", {
    provider: provider,
    flowStage: "CANCEL"
  }).catch((e) => console.error(e))
})

function clearPoll() {
  if (timerRef.value !== null) {
    clearInterval(timerRef.value)
  }
}

function clearState() {
  redirectUri.value = "";
  polling.value = false
  expiresIn.value = null
  timerRef.value = null;
  interval.value = 5;
  lastPollAt.value = -1;
  deviceCode.value = ""
  deviceToken.value = ""
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
          
          <UiButton class="flex! items-center" :icon="faCopy"></UiButton>
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