<script lang="ts" setup>
import {JavaFetch} from '@/core/javaFetch';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import DeviceAuthModal from "@/components/groups/auth/DeviceAuthModal.vue";
import {useAccountsStore} from "@/store/accountsStore.ts";
import {ref} from "vue";

const accountStore = useAccountsStore();
const open = ref(false)

const emit = defineEmits<{
  (event: 'closed'): void;
}>()

async function loadInitialCode() {
  const req: any = await JavaFetch.create(`https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode`)
    .method("POST")
    .contentType("application/x-www-form-urlencoded")
    .body(new URLSearchParams({
      client_id: "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8",
      scope: "offline_access XboxLive.signin" // Same as our own auth service
    }).toString())
    .execute()

  if (!req?.statusCode || req.statusCode !== 200) {
    return {
      error: "Failed to get the code, a retry maybe resolve the issue."
    };
  }

  const data = await req.json();
  // Very verbose checking to ensure we have all the required data
  if (!("user_code" in data) || !("device_code" in data) || !("verification_uri" in data) || !("expires_in" in data)) {
    return { error: "Failed to get the code, a retry maybe resolve the issue." }
  }
  
  return {
    userCode: data.user_code,
    deviceCode: data.device_code,
    verificationUri: data.verification_uri,
    expiresIn: data.expires_in,
  }
}

async function checkForToken(deviceCode: string) {
  try {
    const req: any = await JavaFetch.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
      .method("POST")
      .contentType("application/x-www-form-urlencoded")
      .body(new URLSearchParams({
        client_id: "f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8",
        grant_type: "urn:ietf:params:oauth:grant-type:device_code",
        device_code: deviceCode
      }).toString())
      .execute();

    // We're expecting this error
    if (req?.statusCode === 400) {
      const data = await req.json();
      if (!("error" in data)) {
        return {error: "Failed to get the token, a retry maybe resolve the issue."}
      }

      // We're just waiting for the user to login
      if (data.error === "authorization_pending") {
        return {pass: true}
      }

      if (data.error === "expired_token") {
        return {error: "The token has expired, please try again."}
      }

      if (data.error === "authorization_declined") {
        return {error: "You denied the login request, please try again or close the window."}
      }

      if (data.error === "bad_verification_code") {
        return {error: "Something has gone wrong, please try again."}
      }

      if (data.error === "invalid_scope") {
        return {error: "It looks like the FTB Apps client hasn't been setup correctly, please create an issue on our Github (https://github.com/FTBTeam/FTB-App/issues)"}
      }
    } else {
      const data = await req.json();

      if ("token_type" in data && "access_token" in data && "refresh_token" in data && "expires_in" in data) {
        // We can now use this data to jump through the hops required to get the user logged in
        return { data };
      } else {
        return {error: "Failed to get the token, a retry maybe resolve the issue."};
      }
    }
  } catch (e) {
    console.error(e);
  }
  
  return {error: "Failed to get the token, a retry maybe resolve the issue."};
}

async function continueTokenFlow(data: any) {
  try {
    const res = await sendMessage("profiles.ms.authenticate", {
      liveExpires: data.expires_in as any,
      liveAccessToken: data.access_token as any,
      liveRefreshToken: data.refresh_token as any,
    })
    
    if (!res.success) {
      return { error: `Code: ${res.code} failed due to ${res.message}` }
    }

    await accountStore.loadProfiles();
    return true;
  } catch (e) {
    alertController.error("Failed to login, please try again.");
    return {error: "Failed to login, please try again."}
  }
}
</script>

<template>
  <DeviceAuthModal
    :open="open"
    subtext="Login to Minecraft via your Microsoft account. Clicking the button below will open the Microsoft Login page and copy the code to your clipboard."
    account-type="Microsoft" 
    :load-code="loadInitialCode"
    :check-for-success="checkForToken as any"
    :on-result="continueTokenFlow"
    @closed="emit('closed')"
  />
</template>