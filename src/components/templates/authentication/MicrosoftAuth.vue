<template>
  <div class="ms-auth text-center">
    <h3 class="text-3xl mt-6 mb-4 font-bold">Waiting for response...</h3>
    <div class="logo my-12">
      <img class="mx-auto" src="@/assets/images/branding/microsoft.svg" alt="Microsoft logo" />
    </div>
    <p class="mb-6">
      A browser window should have just popped up. Follow the instructions on screen, once you've finished, you'll see
      that you've been signed in here.
    </p>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import Loading from '@/components/atoms/Loading.vue';
import platform from '@/utils/interface/electron-overwolf';
import { Action } from 'vuex-class';
import { addHyphensToUuid } from '@/utils/helpers';

@Component({
  components: { YggdrasilAuthForm, Loading },
})
export default class MicrosoftAuth extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles!: () => Promise<void>;

  loggedIn = false;
  error = '';
  loading = false;

  mounted() {
    this.openMsAuth();
  }

  // TODO: Move
  async openMsAuth() {
    try {
      this.loading = true;
      const res = await platform.get.actions.openMsAuth();

      if (!res.key || !res.iv || !res.password) {
        return; // TODO: handle error
      }

      const responseRaw: any = await fetch('https://msauth.feed-the-beast.com/v1/retrieve', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          key: res.key,
          iv: res.iv,
          password: res.password,
        }),
      });

      const response: any = (await responseRaw.json()).data;
      const id: string = response.minecraftUuid;
      const newUuid = addHyphensToUuid(id);

      this.sendMessage({
        payload: {
          type: 'profiles.addMs',
          ...response,
          minecraftUuid: id.includes('-') ? id : newUuid,
        },
        callback: async (e: any) => {
          if (e.success) {
            await this.loadProfiles();
            this.$emit('authenticated');
          } else {
            this.$emit('error', "Failed to add new profile, it's likely that you already have this one added");
          }
        },
      });
    } catch (e) {
      console.log(e);
    } finally {
      this.loading = false;
    }
  }
}
</script>

<style lang="scss" scoped></style>
