<template>
  <div class="authentication" @mousedown.self="$emit('close')">
    <div class="body-contents text-center">
      <div class="back" v-if="showLegacyLogin && !loggedIn" @click="showLegacyLogin = false">
        <font-awesome-icon icon="chevron-left" />
        <span>Back to options</span>
      </div>
      <div class="main" v-if="!showLegacyLogin && !loggedIn">
        <h3 class="text-2xl mb-4"><b>Minecraft Login</b></h3>
        <p class="mb-8">
          Now that Minecraft uses Microsoft to login, we now require to login to your Microsoft account or your Mojang
          account.
        </p>

        <h4 class="text-center font-bold mb-8">Sign in with</h4>

        <button class="button" @click="openMsAuth">
          <img src="@/assets/images/branding/microsoft.svg" alt="Microsoft Login" />
        </button>

        <div class="or">
          <span>or</span>
        </div>

        <button class="button" @click="showLegacyLogin = true">
          <img src="@/assets/images/branding/mojang.svg" alt="Mojang Login" />
        </button>
      </div>

      <div class="logged-in" v-else-if="loggedIn">
        You're in!
      </div>

      <yggdrasil-auth-form
        v-else
        @authenticated="
          () => {
            showLegacyLogin = false;
            loggedIn = true;
          }
        "
      />
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/authentication/YggdrasilAuthForm.vue';
import platform from '@/utils/interface/electron-overwolf';
import { authenticateMc } from '@/utils/msauthentication';

@Component({
  components: { YggdrasilAuthForm },
})
export default class Authentication extends Vue {
  showLegacyLogin = false;
  loggedIn = false;

  async openMsAuth() {
    try {
      const res = await platform.get.actions.openMsAuth();
      const response = await authenticateMc(res.code, res.random);

      // TODO: call to the backend and store the entire payload
      console.log(response);
    } catch (e) {
      console.log(e);
    }
  }
}
</script>

<style lang="scss" scoped>
.authentication {
  width: 100%;
  height: 100%;

  display: flex;
  justify-content: center;
  align-items: center;

  position: fixed;
  top: 0;
  left: 0;
  background-color: rgba(#1c1c1c, 0.82);
  backdrop-filter: blur(2px);
  z-index: 1000;

  .body-contents {
    background-color: #313131;
    border-radius: 10px;
    box-shadow: 0 6.6px 22.66px 0 #00000040;
    padding: 3rem;
    width: 590px;
    position: relative;
    z-index: 1;

    .back {
      position: absolute;
      display: flex;
      align-items: center;
      top: 1rem;
      left: 1rem;
      padding: 1rem;
      cursor: pointer;
      transition: background-color 0.25s ease-in-out, opacity 0.25s ease-in-out, max-width 0.25s ease-in-out;
      border-radius: 5px;
      opacity: 0.5;
      max-width: 45px;
      overflow: hidden;

      &:hover {
        opacity: 1;
        background-color: black;
        max-width: 180px;

        span {
          opacity: 1;
        }
      }

      span {
        white-space: nowrap;
        display: inline-block;
        margin-left: 1.5rem;
        opacity: 0;
        transition: opacity 0.15s ease-in-out;
      }
    }

    .button {
      height: 100px;
      background-color: #454545;
      width: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 5px;
      box-shadow: 0 0 0 0 #00000040;
      transition: background-color 0.2s ease-in-out, transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;

      &:focus {
        outline: none;
      }

      &:hover {
        background-color: #575757;
        transform: translateX(0.3rem) translateY(-0.3rem);
        box-shadow: -0.3rem 0.3rem 5px 0 #00000040;
      }
    }

    .or {
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 1.5rem 0;
      position: relative;

      &::after {
        background-color: rgba(white, 0.2);
        content: '';
        position: absolute;
        top: 50%;
        left: 0;
        transform: translatey(-50%);
        height: 2px;
        border-radius: 4px;
        width: 100%;
        z-index: -1;
      }

      span {
        background-color: #313131;
        padding: 0 3rem;
        text-transform: uppercase;
        font-weight: bold;
        color: rgba(white, 0.25);
      }
    }
  }
}
</style>
