<template>
  <div class="ms-auth" :class="{'text-left': somethingIsWrong || receivedSiteResponse}">
    <template v-if="!receivedSiteResponse">
      <div class="logo my-12">
        <img class="mx-auto" src="@/assets/images/branding/microsoft.svg" alt="Microsoft logo" />
      </div>
      
      <h3 class="text-3xl mt-6 mb-4 font-bold" v-if="!somethingIsWrong">Waiting for login<span class="dot" /><span class="dot" /><span class="dot" /></h3>
      <p class="mb-2" v-if="!somethingIsWrong">
        Please login with the window that has opened.<br />
        If no window has opened use the link below.
      </p>
      <a class="link block mb-6 select-text" v-if="!somethingIsWrong" @click="platform.get.utils.openUrl(manualAuthUrl)">{{ manualAuthUrl }}</a>
      
      <ftb-button v-if="!somethingIsWrong" class="inline-block px-6 py-2 font-bold" color="warning" @click="somethingIsWrong = true">Something gone wrong?</ftb-button>
      <div class="something-is-wrong" v-if="somethingIsWrong">
        <h3 class="text-xl font-bold mb-4">Something isn't working correct?</h3>
        <p class="mb-2">Sometimes the app failed to open links for what ever reason, please use the one below, copy if necessary.</p>
        <a class="link block mb-6 select-text" @click="platform.get.utils.openUrl(manualAuthUrl)">{{ manualAuthUrl }}</a>
        <ftb-input v-model="manualCode" :button="true" button-color="primary" :button-click="loginWithCode" label="Authentication Code" placeholder="The code provided by the website." />
      </div>
    </template>
    <div class="pt-4" v-else>
      <h4 class="text-xl font-bold mb-8 text-center">Logging into Minecraft</h4>
      
      <message type="danger" class="mb-8" v-if="failed">
        <div class="error-code select-text">
          <b class="font-mono mb-1 block">{{code.toUpperCase()}}</b>
          <p v-if="code === 'ftb-auth-000001' || code === 'ftb-auth-000002' || code === 'ftb-auth-000003'">We've been unable to communicate with XBox live. It's possible their services are down.</p>
          <p v-else-if="code === 'ftb-auth-000012'">The account used does not have an XBox Live account, this likely means you haven't yet migrated your account...</p>
          <p v-else-if="code === 'ftb-auth-000013'">Your account resided in a region that does not support XBox Live... This means we will not be able to log you in.</p>
          <p v-else-if="code === 'ftb-auth-000014'">The account needs adult verification on Xbox page (typically only seen on South Korean accounts)</p>
          <p v-else-if="code === 'ftb-auth-000015'">Your account is an under 18's account. You will either need to 'correct' your age or have your accounted added to a Family account.</p>
          <p v-else-if="code === 'ftb-auth-000016'">Your account has been rejected by XBox live services for an unknown reason, please forward this error to a member of staff on our <a class="underline" @click="platform.get.utils.openUrl('https://go.ftb.team/discord')">Discord</a></p>
          <p v-else-if="code === 'ftb-auth-000004'">Unable to verify account entitlements, this likely means you've logged in with the incorrect account.</p>
          <p v-else-if="code === 'ftb-auth-000005'">It looks like you own XBox Gamepass for PC or have only recently bought Minecraft Java Edition, this is great! Unfortunately, before we can log you in, you'll need to start the Vanilla Launcher at least once and run Minecraft. Once you have done this, try again</p>
          <p v-else-if="code === 'ftb-auth-000006'">We've been unable to get your Minecraft profile and we've been unable to check your accounts entitlements. This means you almost definitely don't own Minecraft on this account.</p>
          <p v-else-if="code === 'ftb-f-auth-000001'">Unable to load credentials from our systems. Please try again...</p>
          <p v-else-if="code === 'ftb-f-auth-000002'">Failed with an unexpected result, please ask for help on our <a class="underline" @click="platform.get.utils.openUrl('https://go.ftb.team/discord')">Discord</a></p>
          <p v-else>Something has fatally gone wrong! Please ask for help on our <a class="underline" @click="platform.get.utils.openUrl('https://go.ftb.team/discord')">Discord</a></p>
          
          <p v-if="networkError" class="mt-4">It also looks like there could have been network issues involved in this issue.</p>
        </div>
      </message>

      <div class="steps">
        <template v-for="step in steps">
          <div class="step" :key="step.name" v-if="!failed || step.error">
            <div class="main">
              <span>{{ step.name }}</span>
              <p v-if="step.working">Waiting for response</p>
              <p v-else-if="step.error">Something has gone wrong, we can't proceed</p>
              <p v-else-if="step.done">{{randomCongratulatoryEmoji()}}  Finished!</p>
              <p v-else>Waiting for step to start...</p>
            </div>
            <div class="icon" :class="{done: step.done, working: step.working, error: step.error}">
              <font-awesome-icon :fixed-width="true" :icon="step.done ? 'check' : (step.error ? 'times' : (step.working ? 'circle-notch' : 'hourglass'))" :spin="step.working" />
            </div>
          </div>
        </template>
      </div>
      
      <div v-if="failed" class="flex justify-center mt-12">
        <ftb-button class="px-16 py-2" color="info">Try again</ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import platform from '@/utils/interface/electron-overwolf';
import { Action } from 'vuex-class';
import {loginWithMicrosoft} from '@/utils/auth/authentication';
import {emitter} from '@/utils';
import {AuthenticationCredentialsPayload} from '@/core/@types/authentication.types';

function createStep(name: string) {
  return {
    name,
    working: false,
    error: false,
    done: false
  }
}

@Component({
  components: { YggdrasilAuthForm },
})
export default class MicrosoftAuth extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles!: () => Promise<void>;
  
  steps: Record<string, ReturnType<typeof createStep>> = {
    START_DANCE: createStep('Credentials Received'),
    AUTH_XBOX: createStep('Login to XBox'),
    AUTH_XSTS: createStep('Login to XBox Services'),
    LOGIN_XBOX: createStep('Login to Minecraft'),
    GET_PROFILE: createStep('Getting your Profile'),
    CHECK_ENTITLEMENTS: createStep('Checking game ownership')
  }
  
  platform = platform;
  receivedSiteResponse = false;
  failed = false;
  code: string = "";
  networkError = false;

  somethingIsWrong = false;
  manualCode = "";
  
  mounted() {
    platform.get.actions.openMsAuth();
    
    emitter.on("authentication.callback", this.onAuthenticationCallback as any)
    emitter.on('ws.message', this.onStepUpdate);
    console.log("Listening for authentication callback")
  }
  
  destroyed() {
    emitter.off("authentication.callback", this.onAuthenticationCallback as any)
    emitter.off('ws.message', this.onStepUpdate);
    console.log("Closing listener for authentication callback")
  }
  
  async onAuthenticationCallback(credentials?: AuthenticationCredentialsPayload) {
    if (!credentials) {
      this.$emit("error", "Failed to fetch credentials from the website, please try again...")
      // It failed
      return;
    }
    
    await this.handleLogIn(credentials);
  }

  // This is purely visual and it's not a huge issue if it fails
  onStepUpdate(message: any) {
    if (message?.type !== "authenticationStepUpdate" || !message.id) {
      return;
    }

    // Assume it's all good
    this.steps[message.id].working = message.working;
    this.steps[message.id].done = message.successful;
    this.steps[message.id].error = message.error;
  }

  async loginWithCode() {
    if (!this.manualCode) {
      return;
    }

    await this.handleLogIn(this.manualCode);
  }

  async handleLogIn(credentials: AuthenticationCredentialsPayload) {
    this.receivedSiteResponse = true;
    const result = await loginWithMicrosoft(credentials);
    if (!result.success) {
      this.failed = true;
      this.code = result?.code ?? "unknown";
      if (result.networkError) {
        this.networkError = false;
      }

      return;
    }
    
    await this.successDownAndOut();
  }
  
  async successDownAndOut() {
    await this.loadProfiles();
    await platform.get.actions.closeWebservers();
    this.$emit('authenticated');
    this.cleanUp();
  }
  
  cleanUp() {
    this.failed = false;
    this.code = "";
    this.networkError = false;
    this.manualCode = "";
  }
  
  get manualAuthUrl() {
    // zsXz = ?useNew=true // Q9VA = legacy system
    return platform.isOverwolf() ? "https://go.ftb.team/zsXz" : "https://go.ftb.team/Q9VA";
  }
  
  randomCongratulatoryEmoji() {
    const emojis = ["🎉", "🎈", "🎊", "🥳", "🎂", "✅", "🏅", "🚀", "✨", "🍿", "🍾"];
    return emojis[Math.floor(Math.random() * emojis.length)];
  }
}
</script>

<style lang="scss" scoped>
@use "sass:math";

.link {
  text-decoration: underline;
}

.dot {
  $size: 5px;
  $totalTime: 1.8;
  $count: 3;
  display: inline-block;
  background: white;
  
  border-radius: $size;
  width: $size;
  height: $size;
  margin-left: .3rem;
  opacity: 0;
  
  animation: dotsLoop #{$totalTime}s ease-in-out infinite;
  
  &:nth-child(2) {
    animation-delay: #{math.div($totalTime, $count)}s;
  }

  &:nth-child(3) {
    animation-delay: #{(math.div($totalTime, $count)*2)}s;
  }
  
  @keyframes dotsLoop {
    0%, 100% {
      opacity: 0;
    }
    50% {
      opacity: 1;
    }
  }
}

.steps {
  .step {
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    &:not(:last-child) {
      margin-bottom: 1rem;
    }
    
    span {
      font-weight: bold;
      margin-bottom: .4rem;
      display: block;
    }
    
    p {
      opacity: .8;
    }
    
    .icon {
      padding: 0.5rem 0.625rem;
      background-color: var(--color-info-button);
      border-radius: 2rem;
      
      &.working {
        background-color: var(--color-warning-button);
      }
      
      &.done {
        background-color: var(--color-success-button);
      }
      
      &.error {
        background-color: var(--color-danger-button);
      }
    }
  }
}
</style>
