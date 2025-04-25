<script lang="ts" setup>
import DeviceAuthModal from "@/components/groups/auth/DeviceAuthModal.vue";
// import {useAppStore} from "@/store/appStore.ts";
import * as client from 'openid-client'
import {ref} from "vue";

const {
  open
} = defineProps<{
  open: boolean;
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const responseHolder = ref<client.DeviceAuthorizationResponse | null>(null);

async function loadInitialCode() {
  const config = await client.discovery(new URL("https://identity.feed-the-beast.com/realms/FTB"), "ftb-app");
  const response = await client.initiateDeviceAuthorization(config, { scope: "profile" })

  if ("user_code" in response) {
    responseHolder.value = response;
    return {
      userCode: response.user_code,
      deviceCode: response.device_code,
      verificationUri: response.verification_uri,
      expiresIn: response.expires_in,
    }
  } else {
    return {
      error: "Failed to get the code, a retry maybe resolve the issue."
    }
  }
}

async function checkForToken(_: string) {
  const config = await client.discovery(new URL("https://identity.feed-the-beast.com/realms/FTB"), "ftb-app");
  const tokens: client.TokenEndpointResponse = await client.pollDeviceAuthorizationGrant(config, ((responseHolder as any).value! as any))
  
  console.log(tokens)
}

async function continueTokenFlow(data: any) {
  console.log(data);
}
</script>

<template>
  <DeviceAuthModal
    subtext="Login to with your FTB Account. Clicking the button below will open the FTB Login page and copy the code to your clipboard."
    accountType="FTB"
    :open="open"
    :loadCode="loadInitialCode"
    :checkForSuccess="checkForToken as any"
    :onResult="continueTokenFlow as any"
    @closed="emit('closed')"
  />
</template>