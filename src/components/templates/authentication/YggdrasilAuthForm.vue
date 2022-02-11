<template>
  <div class="legacy-auth">
    <img src="@/assets/images/branding/mojang.svg" alt="Mogang logo" class="mb-8 mt-4 mx-auto" />
    <p class="mb-10" v-if="uuid && uuid.length > 0">
      Your session has expired, please login to your Mojang account again to continue playing!
    </p>
    <p class="mb-10" v-else>
      If you have not yet migrated your account a Microsoft account, you can still log in with your username and
      password.
    </p>

    <div class="error bg-red-400 text-red-900 px-4 py-2 rounded mb-8" v-if="error">
      <strong>{{ error }}</strong>
    </div>

    <form @submit.prevent="submit">
      <ftb-input
        label="Username / Email address"
        class="text-left mb-6"
        v-model="username"
        placeholder="myminecraftusername"
      />
      <ftb-input label="Password" type="password" class="text-left mb-8" v-model="password" placeholder="pass****" />

      <button
        type="submit"
        class="mb-6 rounded py-4 bg-green-600 ftb-button block w-full hover:bg-green-500"
        :class="{ 'disabled opacity-50 cursor-not-allowed': username === '' || password === '' }"
        :disabled="username === '' || password === ''"
      >
        Login
      </button>
    </form>
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
import { Prop } from 'vue-property-decorator';

// TODO: remove this after April 2022
@Component({ props: { uuid: String } })
export default class YggdrasilAuthForm extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles: any;

  @Prop()
  public uuid!: string | null;

  error = '';

  username: string = '';
  password: string = '';

  async submit() {
    this.error = '';

    if (this.username === '' || this.password === '') {
      this.error = 'Please fill in all fields.';
      return;
    }

    this.sendMessage({
      payload: {
        type: 'profiles.mc.authenticate',
        username: this.username,
        password: this.password,
      },
      callback: (data: any) => {
        let json = {} as any;
        try {
          json = JSON.parse(data.response);
        } catch (e) {
          this.error = 'Invalid response from Mojang.';
          return;
        }

        if (json.error || !json) {
          this.error = json.errorMessage || 'Unknown error.';
          return; // TODO: handle error
        }

        const id: string = json.selectedProfile.id;
        const newUuid = addHyphensToUuid(id);

        this.sendMessage({
          payload: {
            type: this.uuid ? 'profiles.updateMc' : 'profiles.addMc',
            uuid: this.uuid ? this.uuid : undefined,
            username: json.selectedProfile.name,
            accessToken: json.accessToken,
            clientToken: json.clientToken,
            userUuid: id.includes('-') ? id : newUuid,
          },
          callback: async (e: any) => {
            if (e.success) {
              await this.loadProfiles();
              this.$emit('authenticated');
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
