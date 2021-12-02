<template>
  <div class="legacy-auth">
    <img src="@/assets/images/branding/mojang.svg" alt="Mogang logo" class="mb-8 mt-4 mx-auto" />
    <p class="mb-12">
      If you have not yet migrated your account a Microsoft account, you can still log in with your username and
      password.
    </p>

    <!-- TODO: make me look nice -->
    <code v-if="error">{{ error }}</code>

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
import { Action } from 'vuex-class';
import { addHyphensToUuid } from '@/utils/helpers';

@Component
export default class YggdrasilAuthForm extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles: any;

  error = '';

  username: string = '';
  password: string = '';

  async submit() {
    this.sendMessage({
      payload: {
        type: 'profiles.mc.authenticate',
        username: this.username,
        password: this.password,
      },
      callback: (data: any) => {
        if (!data.success) {
          return; // TODO: handle error
        }

        const json = JSON.parse(data.response);
        const id: string = json.selectedProfile.id;
        const newUuid = addHyphensToUuid(id);

        this.sendMessage({
          payload: {
            type: 'profiles.addMc',
            username: json.selectedProfile.name,
            accessToken: json.accessToken,
            clientToken: json.clientToken,
            userUuid: id.includes('-') ? id : newUuid,
          },
          callback: (e: any) => {
            if (e.success) {
              this.$emit('authenticated');
              this.loadProfiles();
            } else {
              this.error = 'Something went wrong';
            }
          },
        });
      },
    });
  }
}
</script>

<style lang="scss" scoped></style>
