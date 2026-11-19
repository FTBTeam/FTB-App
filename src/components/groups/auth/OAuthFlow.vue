<script setup lang="ts">
import {Loader, Message, UiButton} from "@/components/ui";
import {onMounted, onUnmounted, ref} from "vue";
import {AccountProvider} from "@/core/types/javaApi";
import appPlatform from '@platform'
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {
  faClock,
  faCopy, faDesktop,
  faExternalLink,
} from "@fortawesome/free-solid-svg-icons";

const {
  provider
} = defineProps<{
  provider: AccountProvider
}>()

const emit = defineEmits<{
  (event: 'started', flowId: string): void;
}>()

const loading = ref(true)
const error = ref<string | null>(null);

const redirectUri = ref<string>("");
const flowId = ref<string | null>(null);

let unmounted = false;

onMounted(() => {
  startFlow();
})

onUnmounted(() => {
  unmounted = true;
  if (flowId.value) {
    cancel(flowId.value);
  }
})

function cancel(id: string) {
  sendMessage('accounts.oauth.cancel', {
    provider: provider,
    flowId: id
  }).catch((e) => console.error(e))
}

async function startFlow() {
  loading.value = true;
  try {
    const result = await sendMessage('accounts.oauth.start', {
      provider: provider
    })
    
    if (unmounted) {
      if (result.flowId) {
        cancel(result.flowId);
      }
      return;
    }

    // Rip
    if (!result.success || !result.url || !result.flowId) {
      redirectUri.value = "";
      error.value = result.reason ?? "Failed to start the sign in. Please try again.";
      return;
    }

    flowId.value = result.flowId;
    redirectUri.value = result.url;
    emit('started', result.flowId);
    appPlatform.utils.openUrl(result.url);
  } catch (e) {
    console.error(e);
    error.value = "Failed to start the sign in. Please try again.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div v-if="!loading && !error">
    <div class="bg-black/20 border border-white/10 rounded-lg px-6 py-14 flex items-center justify-center flex-col gap-6 text-center">
      <div class="flex gap-8 mb-10">
        <FontAwesomeIcon size="2xl" :icon="faDesktop" />
        <div class="flex gap-2 items-center">
          <div v-for="n in 8" :key="n" class="w-2 h-2 bg-white rounded-full opacity-0 animate-pulse" :style="`animation-delay: ${100 * n}ms`"></div>
        </div>
        <img v-if="provider === 'MICROSOFT'" width="26" src="../../../assets/images/branding/microsoft-squares.webp" />
        <img v-if="provider === 'FTB'" width="40" src="../../../assets/images/ftb-logo.svg" />
      </div>
      <div class="flex-1">
        <p class="font-bold text-lg mb-2">Continue in your browser</p>
        <p class="text-white/80">We've opened the sign-in page in a new browser window. This window updates by itself when you're done.</p>
      </div>
    </div>
    
    <div class="flex gap-2 mt-4 mb-10">
      <UiButton @click="appPlatform.utils.openUrl(redirectUri)" :icon="faExternalLink">Open again</UiButton>
      <UiButton :icon="faCopy">Copy Link</UiButton>
    </div>
    
    <Message type="warning" :icon="faClock">
      Taking a while? Something might be wrong... Try using the Device Code login instead.
    </Message>
  </div>
  
  <Message type="danger" title="Something went wrong!" v-if="error && !loading">
    {{ error }}
  </Message>
  
  <Loader v-if="loading" />
</template>