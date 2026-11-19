<script setup lang="ts">
import {ref} from "vue";
import {Modal} from "@/components/ui";
import {AccountProvider} from "@/core/types/javaApi";
import DeviceAuthModal2 from "@/components/groups/auth/DeviceAuth.vue";
import OAuthFlowModel from "@/components/groups/auth/OAuthFlow.vue";
import {faMobile} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

type AuthType = 'device' | 'oauth'

const {
  open,
  accountType
} = defineProps<{
  accountType: AccountProvider
  open: boolean,
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const selectedType = ref<AuthType>('oauth');

function switchType() {
  selectedType.value = selectedType.value === 'oauth' ? 'device' : 'oauth'
}

function close() {
  selectedType.value = 'oauth';
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
    
    <DeviceAuthModal2 :provider="accountType" v-if="selectedType === 'device'" />
    <OAuthFlowModel :provider="accountType" v-if="selectedType === 'oauth'" />
    
    <template #footer>
      <div class="py-4 flex gap-4 items-center text-blue-300 hover:text-blue-400 cursor-pointer font-bold" @click="switchType">
        <FontAwesomeIcon :icon="faMobile" /> Use {{selectedType === 'oauth' ? 'a code' : 'browser'}} instead
      </div>
    </template>
  </Modal>
</template>