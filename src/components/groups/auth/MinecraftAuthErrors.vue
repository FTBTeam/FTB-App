<template>
  <message v-if="!code || !richErrors[code]" type="danger" class="select-text">
    <b class="mb-2 block">Something has gone wrong!</b>
    {{ error }}
  </message>
  
  <div v-else class="flex flex-col items-center justify-center gap-4 pt-12">    
    <FontAwesomeIcon size="4x" class="mb-4" :icon="richErrors[code].icon" />
    <h2 class="text-2xl font-bold">{{ richErrors[code].title }}</h2>
    <div class="wysiwyg mb-12 mx-auto text-center max-w-[90%]" v-html="parseMarkdown(richErrors[code].descriptionMarkdown)" />
    <div v-if="richErrors[code].button" class="grid grid-cols-2 gap-2 w-full" :class="{'!grid-cols-1': richErrors[code].button?.length === 1}">
      <a class="block w-full" v-for="(button, key) in richErrors[code].button" :key="key" :href="button.link" target="_blank">
        <UiButton class="w-full">
          {{ button.text }}
        </UiButton>
      </a>
    </div>
    
    <div class="flex items-center justify-center gap-4 w-full opacity-50 py-2">
      <span class="h-0.5 rounded bg-white/30 block flex-1" />
      <span class="font-bold">OR</span>
      <span class="h-0.5 rounded bg-white/30 block flex-1" />
    </div>
  </div>
</template>

<script setup lang="ts">
import {IconDefinition} from "@fortawesome/free-brands-svg-icons";
import {
  faBan,
  faChildReaching,
  faFaceSadTear,
  faFileSignature,
  faGavel,
  faGlobe,
  faLock,
  faPersonCircleQuestion,
  faQuestion,
  faTriangleExclamation,
  faUserPlus,
  faWifi
} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {parseMarkdown} from "@/utils";
import {Message, UiButton} from "@/components/ui";

type Btn = {
  text: string,
  link: string,
}

type RichError = Record<string, {
  icon: IconDefinition,
  title: string,
  descriptionMarkdown: string,
  button?: Btn[]
}>;

const troubleShootingButton: Btn = {
  text: "Troubleshooting guide",
  link: "https://go.ftb.team/docs/login-issues",
}

// Microsoft or Minecraft sent back something we couldn't use (auth:90 to auth:98)
const badResponse: RichError[string] = {
  icon: faTriangleExclamation,
  title: "Unexpected response",
  descriptionMarkdown: "Microsoft or Minecraft sent back a response we didn't expect. This is usually a temporary problem on their end, so please try again in a few minutes.",
  button: [troubleShootingButton]
}

const richErrors: RichError = {
  // XBOX_ACCOUNT_MISSING
  "auth:50": {
    icon: faUserPlus,
    title: "No Xbox profile",
    descriptionMarkdown: "This Microsoft account doesn't have an Xbox profile yet, which Minecraft needs. Sign in to Xbox once with this account to create one, then try again.\n\n**If you own Minecraft on a different Microsoft account, sign in with that one instead.**",
    button: [troubleShootingButton, {
      text: "Create an Xbox profile",
      link: "https://www.xbox.com/live",
    }]
  },
  // XBOX_INVALID_REGION
  "auth:51": {
    icon: faGlobe,
    title: "Restricted Region",
    descriptionMarkdown: "Your account is in a region that is not supported by this game. This might be a mistake or you might be using a VPN, (or not using a VPN 😉)",
    button: [troubleShootingButton, {
      text: "See Xbox support article",
      link: "https://go.ftb.team/app/xbox-region",
    }]
  },
  // XBOX_ADULT_VERIFICATION
  "auth:52": {
    icon: faBan,
    title: "Adult Verification Required",
    descriptionMarkdown: "Family linked accounts for users under 18 will require approval before you can use our service. You can find a guide on how to have you account approved below",
    button: [{
      text: "How to get approved",
      link: "https://go.ftb.team/app/microsoft-parental-consent",
    }]
  },
  // XBOX_UNDER_18
  "auth:53": {
    icon: faBan,
    title: "Under 18",
    descriptionMarkdown: "Your account is under 18 years old, and cannot use our service. If this is incorrect, make sure you've updated your date of birth on your Microsoft account to be over 18.",
    button: [{
      text: "Update Microsoft account date of birth",
      link: "https://go.ftb.team/app/microsoft-profile-update"
    }]
  },
  // XBOX_BANNED
  "auth:54": {
    icon: faGavel,
    title: "Account banned",
    descriptionMarkdown: "This account has been banned from Xbox, so it can't be used to play Minecraft. You can see the details of the ban, and appeal it, on the Xbox enforcement page.",
    button: [{
      text: "Xbox enforcement",
      link: "https://enforcement.xbox.com",
    }]
  },
  // XBOX_CHILD_RESTRICTED
  "auth:55": {
    icon: faChildReaching,
    title: "Parental controls",
    descriptionMarkdown: "This is a child account, and its parental controls don't allow online play. An adult in your Microsoft family needs to allow it for this account, then you can try again.",
    button: [{
      text: "Microsoft family settings",
      link: "https://account.microsoft.com/family",
    }]
  },
  // XBOX_TERMS_NOT_ACCEPTED
  "auth:56": {
    icon: faFileSignature,
    title: "Xbox terms not accepted",
    descriptionMarkdown: "This account hasn't accepted the Xbox terms of use yet. Sign in to Xbox once with this account and accept them, then try again.",
    button: [troubleShootingButton, {
      text: "Sign in to Xbox",
      link: "https://www.xbox.com/live",
    }]
  },
  // XBOX_UNKNOWN_FAILURE_CODE
  "auth:60": {
    icon: faQuestion,
    title: "Xbox wouldn't sign you in",
    descriptionMarkdown: "Xbox refused to sign this account in, but didn't tell us why. Signing in at xbox.com with the same account usually shows what's wrong.",
    button: [troubleShootingButton, {
      text: "Sign in to Xbox",
      link: "https://www.xbox.com/live",
    }]
  },
  // MISSING_CLAIMS
  "auth:70": {
    icon: faQuestion,
    title: "Xbox profile incomplete",
    descriptionMarkdown: "Xbox signed you in, but didn't send back the details Minecraft needs. Signing in at xbox.com once usually fixes this, then try again.",
    button: [troubleShootingButton]
  },
  // MISSING_ENTITLEMENTS
  "auth:71": {
    icon: faPersonCircleQuestion,
    title: "Game not owned",
    descriptionMarkdown: "We're unable to find your Minecraft profile and after checking you `entitlements` _(a list of games you own)_ we can't find Minecraft.\n\n**It's very likely you've logged into the wrong Microsoft account**",
    button: [
      troubleShootingButton,
      {
        text: "Buy Minecraft",
        link: "https://www.minecraft.net/",
      }
    ]
  },
  // INVALID_PROFILE
  "auth:72": {
    icon: faQuestion,
    title: "Unable to find Minecraft profile",
    descriptionMarkdown: "We were able to verify your Minecraft account as your profile was not found.\n\n**It's very likely you've logged into the wrong Microsoft account**",
    button: [
      troubleShootingButton,
      {
        text: "Buy Minecraft",
        link: "https://www.minecraft.net/",
      }
    ]
  },
  // REQUEST_REJECTED
  "auth:80": {
    icon: faFaceSadTear,
    title: "Request Rejected",
    descriptionMarkdown: "Microsoft / Minecraft has rejected your authentication request! This could be caused by a number of things, or it's a temporary issue on their end. See our troubleshooting guide below for more information.",
    button: [
      troubleShootingButton
    ]
  },
  // TOKEN_STORAGE_FAILED
  "auth:81": {
    icon: faLock,
    title: "Couldn't save your account",
    descriptionMarkdown: "You signed in, but we couldn't save your account securely. This is usually caused by the system keychain or credential store being locked or unavailable. Unlock it, or restart the app, then try again.",
    button: [troubleShootingButton]
  },
  // COMPLETE_REQUEST_FAILURE
  "auth:99": {
    icon: faWifi,
    title: "Couldn't reach Microsoft",
    descriptionMarkdown: "We couldn't connect to Microsoft or Minecraft's servers. Check your internet connection, and that a firewall, VPN or proxy isn't blocking the app.",
    button: [troubleShootingButton]
  },
  "auth:90": badResponse,
  "auth:94": badResponse,
  "auth:95": badResponse,
  "auth:96": badResponse,
  "auth:97": badResponse,
  "auth:98": badResponse,
}

const {
  code,
  error,
} = defineProps<{
  code?: string;
  error: string;
}>()
</script>