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
  faFaceSadTear,
  faGlobe,
  faPersonCircleQuestion,
  faQuestion
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

const richErrors: RichError = {
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
}

const {
  code,
  error,
} = defineProps<{
  code?: string;
  error: string;
}>()
</script>