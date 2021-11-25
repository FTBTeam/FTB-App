<template>
  <div class="legacy-auth">
    <img src="@/assets/images/branding/mojang.svg" alt="Mogang logo" class="mb-8 mt-4 mx-auto" />
    <p class="mb-12">
      If you have not yet migrated your account a Microsoft account, you can still log in with your username and
      password.
    </p>

    <ftb-input
      label="Username / Email address"
      class="text-left mb-6"
      v-model="username"
      placeholder="myminecraftusername"
    />
    <ftb-input
      label="Password"
      type="password"
      class="text-left mb-8"
      v-model="password"
      placeholder="mypass********"
    />

    <ftb-button class="mb-6 rounded py-4 bg-green-500" :disabled="username === '' || password === ''" @click="submit"
      >Login</ftb-button
    >
    <small
      >We communicate directly to the Minecraft Authentication server.<br />We <b>do not</b> store your password.</small
    >
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';

@Component
export default class YggdrasilAuthForm extends Vue {
  username: string = '';
  password: string = '';

  submit() {
    console.log(this.username, this.password);
  }
}
</script>

<style lang="scss" scoped></style>
