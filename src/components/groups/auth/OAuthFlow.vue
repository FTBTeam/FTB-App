<script setup lang="ts">
import {Loader, Message, UiButton} from "@/components/ui";
import {onMounted, onUnmounted, ref} from "vue";
import {AccountProvider} from "@/core/types/javaApi";
import appPlatform from '@platform'
import {useAppStore} from "@/store/appStore.ts";
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

const appStore = useAppStore();

const loading = ref(true)
const error = ref<string | null>(null);

const redirectUri = ref<string>("");

onMounted(() => {
  startFlow();

  appStore.emitter.on('ws/message', handleData)
})

onUnmounted(() => {
  // TODO: Tell the backend to fuckoff
  appStore.emitter.off('ws/message', handleData)
})

function handleData(data: any) {
  console.log(data);
}

async function startFlow() {
  loading.value = true;
  try {
    // const result = await sendMessage('accounts.oauth.start', {
    //   provider: provider
    // })
    //
    // if (!result.success) {
    //   redirectUri.value = "";
    //   error.value = result.reason;
    //   return;
    // }
    //
    // redirectUri.value = result.url;
    // appPlatform.utils.openUrl(result.url);
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
  
  <div v-if="error && !loading">
    {{ error }}
  </div>
  
  <Loader v-if="loading" />
</template>