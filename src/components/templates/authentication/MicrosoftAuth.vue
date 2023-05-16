<template>
  <div class="ms-auth" :class="{'text-left': somethingIsWrong}">
    <template v-if="!loading">
      <div class="logo my-12">
        <img class="mx-auto" src="@/assets/images/branding/microsoft.svg" alt="Microsoft logo" />
      </div>
      
      <h3 class="text-3xl mt-6 mb-4 font-bold" v-if="!somethingIsWrong">Waiting for login<span class="dot" /><span class="dot" /><span class="dot" /></h3>
      <p class="mb-6" v-if="!somethingIsWrong">
        Please login with the window that has opened. If no window has opened, <a class="link" @click="platform.get.utils.openUrl('https://msauth.feed-the-beast.com?useNew')">Click here</a>.
      </p>
      
      <ftb-button v-if="!somethingIsWrong" class="inline-block px-4 py-1" color="warning" @click="somethingIsWrong = true">Something gone wrong?</ftb-button>
      <div class="something-is-wrong" v-if="somethingIsWrong">
        <h3 class="text-xl font-bold mb-4">Something isn't working correct?</h3>
        <p class="mb-6">Damn! That's not good. Hopefully we can resolve it. If you logged in using the website that popped up you should be able to use the code from the website in the box below. If the website did not open, you can <a class="link" @click="platform.get.utils.openUrl('https://msauth.feed-the-beast.com?useNew')">Click here</a> to open it.</p>
        <ftb-input v-model="manualCode" :button="true" button-color="primary" :button-click="loginWithCode" label="Authentication Code" placeholder="The code provided by the website." />
      </div>
    </template>
    <template v-else>
      <h3 class="text-3xl mt-6 mb-4 font-bold">Loading</h3>
      <Loading class="my-4" size="8" />
      <p class="mb-6">We're just checking the credentials, one moment please.</p>
    </template>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import Loading from '@/components/atoms/Loading.vue';
import platform from '@/utils/interface/electron-overwolf';
import { Action } from 'vuex-class';
import {loginWithMicrosoft} from '@/utils/auth/authentication';
import store from '@/modules/store';

@Component({
  components: { YggdrasilAuthForm, Loading },
})
export default class MicrosoftAuth extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles!: () => Promise<void>;
  @Action('showAlert') public showAlert: any;
  
  loading = false;
  platform = platform;

  somethingIsWrong = false;
  manualCode = "";
  
  mounted() {
    this.openMsAuth();
  }
  
  async openMsAuth() {
    platform.get.actions.openMsAuth(({status, success}) => {
      if (status === "processing") {
        this.loading = true;
        return;
      }
      
      if (!success) {
        this.$emit('error', 'Failed to retrieve essential information, please try again.');
        return;
      }
      
      this.$emit('authenticated');
    });
  }

  async loginWithCode() {
    if (!this.manualCode) {
      return;
    }
    
    this.loading = true;
    const result = await loginWithMicrosoft(this.manualCode);
    if (result.success) {
      await this.loadProfiles();
      await platform.get.actions.closeWebservers();
      await platform.get.actions.closeAuthWindow("done", true);
      this.$emit('authenticated');
      this.loading = false;
      return;
    }

    this.loading = false;
    this.$emit('error', `Failed to login due to: ${result.response}`);
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
</style>
