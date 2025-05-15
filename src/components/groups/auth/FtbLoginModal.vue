<script lang="ts" setup>
import DeviceAuthModal from "@/components/groups/auth/DeviceAuthModal.vue";
import * as client from 'openid-client'
import {alertController} from "@/core/controllers/alertController.ts";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import {
  CheckForCodeReturn,
  DeviceCodeHolder,
  LoadCodeReturn,
  OnResultReturn,
} from "@/components/groups/auth/LoginTypes.ts";
import {createLogger} from "@/core/logger.ts";
import {useAccountsStore} from "@/store/accountsStore.ts";
import {getOrCreateOauthClient} from "@/utils";

const {
  open
} = defineProps<{
  open: boolean;
}>()

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

const logger = createLogger("FTBLoginModal.vue")
const accountStore = useAccountsStore();

async function loadInitialCode(): Promise<LoadCodeReturn> {
  const response = await client.initiateDeviceAuthorization(await getOrCreateOauthClient(), { scope: "openid email profile offline_access" })
  
  if ("user_code" in response) {
    return response satisfies DeviceCodeHolder
  }
  
  return {
    error: "Failed to get the code, a retry maybe resolve the issue."
  }
}

async function checkForToken(deviceData: DeviceCodeHolder): Promise<CheckForCodeReturn> {
  try {
    const tokens = await client.pollDeviceAuthorizationGrant(await getOrCreateOauthClient(), deviceData)
    if (tokens.id_token) {
      return { data: tokens }
    }
  } catch (e) {
    // TODO: This is fatal! We need to stop the flow here!
    logger.warn("Error while polling for token", e);
  }
  
  return { pass: true }
}

async function continueTokenFlow(data: any): Promise<OnResultReturn> {
  if (!data.id_token) {
    return { error: "Failed to get the token, a retry maybe resolve the issue." }
  }
  
  const typedData = data as client.TokenEndpointResponse;
  try {
    const res = await sendMessage("accounts.store-oauth", {
      token: typedData.access_token,
      idToken: typedData.id_token!,
      refreshToken: typedData.refresh_token!,
      expiresIn: typedData.expires_in!,
      refreshExpiresIn: data.refresh_expires_in!,
      notViableForLogging: true
    })
    
    if (res.success) {
      // Use the token & the config to get the user info
      accountStore.ftbAccountManager.init(res.completeToken).catch((e) => logger.error(e))
    }
  } catch (e) {
    logger.error("Unable to continue token flow", e)
    alertController.error("Failed to store the token, a retry maybe resolve the issue.")
  }
  
  return {error: "Failed to store the token, a retry maybe resolve the issue."}
}
</script>

<template>
  <DeviceAuthModal
    subtext="Login to with your FTB Account. Clicking the button below will open the FTB Login page and copy the code to your clipboard."
    accountType="FTB"
    :open="open"
    :requires-polling="false"
    :show-qr-code="false"
    :loadCode="loadInitialCode"
    :checkForSuccess="checkForToken"
    :onResult="continueTokenFlow"
    @closed="emit('closed')"
  />
</template>