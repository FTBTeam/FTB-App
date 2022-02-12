<template>
  <div class="ms-auth text-center">
    <template v-if="!loading">
      <h3 class="text-3xl mt-6 mb-4 font-bold">Waiting for response...</h3>
      <div class="logo my-12">
        <img class="mx-auto" src="@/assets/images/branding/microsoft.svg" alt="Microsoft logo" />
      </div>
      <p class="mb-6">
        A window should have just popped up. Follow the instructions on screen, once you've finished, you'll see that
        you've been signed in here.
      </p>
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
import { addHyphensToUuid } from '@/utils/helpers';
import { finishAuthentication } from '@/utils/auth/msAuthentication';

export const issueCodes: Record<string, string> = {
  '000001': "We can't verify with Microsoft, looks like somethings gone wrong!",
  '000002': 'Somethings gone wrong with checking your login with Xbox live!',
  '000003': 'Validating your Xbox live session has gone wrong...',
  '000004': "We couldn't authenticate against Minecrafts servers...",
  '000005': "We can't check your game ownership at the moment...",
  '000006':
    "We can't pull your Minecraft profile! This normally means we can't find an account on this Microsoft account",
  '000007': "Oh no, it looks like your account doesn't own Minecraft!",
};

@Component({
  components: { YggdrasilAuthForm, Loading },
})
export default class MicrosoftAuth extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles!: () => Promise<void>;

  loading = false;

  mounted() {
    this.openMsAuth();
  }

  async openMsAuth() {
    try {
      const res = await platform.get.actions.openMsAuth();

      this.loading = true;
      if (!res || !res.key || !res.iv || !res.password) {
        this.$emit('error', 'Unable to authenticate. Please try again.');
        return;
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

      // Use the new flow
      const authRes = await finishAuthentication(
        response.liveAccessToken,
        response.liveRefreshToken,
        response.liveExpires,
      );

      if (!authRes || authRes.code) {
        this.$emit(
          'error',
          !authRes
            ? 'Fatal error whilst trying to retrieve your account details, please try again.'
            : issueCodes[authRes.code],
        );
        return;
      }

      const id: string = authRes.minecraftUuid ?? '';
      const newUuid = addHyphensToUuid(id);

      this.sendMessage({
        payload: {
          type: 'profiles.addMs',
          ...authRes,
          minecraftUuid: id.includes('-') ? id : newUuid,
        },
        callback: async (e: any) => {
          if (e.success) {
            await this.loadProfiles();
            this.$emit('authenticated');
          } else {
            if (e.status === 'profile_exists') {
              this.$emit('error', 'This profile already exists, please try another account.');
            } else {
              this.$emit('error', 'Failed to add new profile... Please try again.');
            }
          }
        },
      });
    } catch (e) {
      console.log(e);
      this.$emit('error', 'Fatal error whilst trying to retrieve your account details, please try again.');
    } finally {
      this.loading = false;
    }
  }
}
</script>

<style lang="scss" scoped></style>
