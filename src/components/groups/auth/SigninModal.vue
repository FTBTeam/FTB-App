<script setup lang="ts">
import {onMounted, onUnmounted, ref} from "vue";
import {Message, Modal, UiButton} from "@/components/ui";
import {AccountProvider, LoginFlowEvent, LoginStep} from "@/core/types/javaApi";
import DeviceAuthModal2 from "@/components/groups/auth/DeviceAuth.vue";
import OAuthFlowModel from "@/components/groups/auth/OAuthFlow.vue";
import {faCircleNotch, faMobile} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {useAppStore} from "@/store/appStore.ts";
import {useAccountsStore} from "@/store/accountsStore.ts";
import {alertController} from "@/core/controllers/alertController.ts";
import MinecraftAuthErrors from "@/components/groups/auth/MinecraftAuthErrors.vue";

type AuthType = 'device' | 'oauth'
type Phase = 'choose' | 'working' | 'failed'

const stepLabels: Record<LoginStep, string> = {
  EXCHANGING_CODE: "Finishing signing in",
  XBOX: "Signing in to Xbox Live",
  XSTS: "Getting access to Xbox Live",
  MINECRAFT: "Signing in to Minecraft",
  PROFILE: "Checking you own Minecraft",
  SAVING: "Saving your account",
}

const {
  open,
  accountType,
  accountId = null
} = defineProps<{
  accountType: AccountProvider
  open: boolean,
  // The account being signed back in, if any
  accountId?: string | null,
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const appStore = useAppStore();
const accountsStore = useAccountsStore();

const selectedType = ref<AuthType>('oauth');
const phase = ref<Phase>('choose');
const step = ref<LoginStep | null>(null);
const failure = ref<string | null>(null);
const failureCode = ref<string | null>(null);

// The flow the user is looking at, events for any other flow are ignored
const flowId = ref<string | null>(null);

onMounted(() => {
  appStore.emitter.on('ws/message', onMessage)
})

onUnmounted(() => {
  appStore.emitter.off('ws/message', onMessage)
})

function onMessage(data: any) {
  if (data?.type !== "event.accounts.login") {
    return;
  }

  const event = data as LoginFlowEvent;

  if (event.status === 'COMPLETE') {
    (event.provider === 'FTB' ? accountsStore.ftbAccountChanged() : accountsStore.loadProfiles())
      .catch(e => console.error(e));
  }

  if (!open || event.provider !== accountType || event.flowId !== flowId.value) {
    return;
  }

  switch (event.status) {
    case 'WORKING':
      phase.value = 'working';
      step.value = event.step ?? null;
      break;
    case 'COMPLETE':
      alertController.success(event.accountName
        ? `Signed in as ${event.accountName}`
        : `Signed in to your ${event.provider === 'MICROSOFT' ? 'Microsoft' : 'FTB'} account`);
      close();
      break;
    case 'FAILED':
      phase.value = 'failed';
      failure.value = event.message ?? "Something went wrong signing in. Please try again.";
      failureCode.value = event.errorCode ?? null;
      break;
  }
}

function onStarted(id: string) {
  flowId.value = id;
}

function onDeviceCompleted() {
  // The backend takes over from here, its events say how it's going
  phase.value = 'working';
  step.value = null;
}

function tryAgain() {
  reset();
}

function switchType() {
  selectedType.value = selectedType.value === 'oauth' ? 'device' : 'oauth'
}

function reset() {
  phase.value = 'choose';
  step.value = null;
  failure.value = null;
  failureCode.value = null;
  flowId.value = null;
}

function close() {
  selectedType.value = 'oauth';
  reset();
  emit('closed')
}
</script>

<template>
  <Modal :open="open" title="" @closed="() => close()">
    <div class="mb-8">
      <div class="flex gap-4 items-center mb-4">
        <img v-if="accountType === 'MICROSOFT'" width="26" src="../../../assets/images/branding/microsoft-squares.webp" />
        <img v-if="accountType === 'FTB'" width="40" src="../../../assets/images/ftb-logo.svg" />
        <p class="text-3xl font-bold">{{accountType === 'MICROSOFT' ? "Sign in to play" : 'Login'}}</p>
      </div>
      <p class="text-white/80 max-w-3/4 leading-6">{{accountType === 'MICROSOFT' ? "Use your Microsoft account that owns Minecraft: Java Edition to play Minecraft" : "Connect your FTB Account to get access to specific perks and features. This will not log you into Minecraft"}}</p>
    </div>

    <template v-if="phase === 'choose'">
      <DeviceAuthModal2 :provider="accountType" v-if="selectedType === 'device'" @started="onStarted" @completed="onDeviceCompleted" />
      <OAuthFlowModel :provider="accountType" :account-id="accountId" v-if="selectedType === 'oauth'" @started="onStarted" />
    </template>

    <div v-if="phase === 'working'" class="bg-black/30 rounded-lg p-4 flex gap-4 items-center">
      <FontAwesomeIcon :icon="faCircleNotch" spin fixedWidth />
      <div class="flex-1">
        <p>Logging you in, won't be a moment...</p>
        <p class="text-white/80 text-sm" v-if="step">{{ stepLabels[step] }}</p>
      </div>
    </div>

    <div v-if="phase === 'failed'">
      <MinecraftAuthErrors v-if="failureCode" :code="failureCode" :error="failure ?? ''" class="mb-4" />
      <Message v-else type="danger" title="Couldn't sign you in" class="mb-4">
        {{ failure }}
      </Message>
      <UiButton type="primary" @click="tryAgain">Try again</UiButton>
    </div>

    <template #footer v-if="phase === 'choose'">
      <div class="py-4 flex gap-4 items-center text-blue-300 hover:text-blue-400 cursor-pointer font-bold" @click="switchType">
        <FontAwesomeIcon :icon="faMobile" /> Use {{selectedType === 'oauth' ? 'a code' : 'browser'}} instead
      </div>
    </template>
  </Modal>
</template>
