<template>
  <modal :open="internalOpen" :title="title[0]" :subTitle="title[1]" @closed="close" :closeOnBackgroundClick="!loading">
    <div class="share-message" v-if="!error && !loading && !shareCode">
      <p>
        You're about to share your Modpack with anyone that you provide the share link with. Once you have shared your
        instance, you can not remove it!
      </p>
      <p class="mt-4">
        <em
          >Your instance will be uploaded to the cloud as is. This means any changes you make after uploading the
          instance will not be shared.</em
        >
      </p>
    </div>
    <div class="loading" v-else-if="loading">
      <progress-bar progress="0" />
    </div>
    <div class="share-code" v-else-if="shareCode">
      <ftb-input :value="shareCode" label="Your share code" :copyable="true" :disabled="true" />
    </div>
    <message type="danger" v-else>
      {{ error }}
    </message>
    <template #footer v-if="!shareCode">
      <div class="flex justify-end">
        <ftb-button
          :disabled="loading"
          class="py-2 px-4"
          color="primary"
          css-class="text-center text-l"
          @click="shareInstance"
        >
          <font-awesome-icon icon="upload" class="mr-2" size="1x" />
          Share
        </ftb-button>
      </div>
    </template>
  </modal>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import { validateAuthenticationOrSignIn } from '@/utils/auth/authentication';
import {sendMessage} from '@/core/websockets/websocketsApi';

@Component({
  components: { ProgressBar },
})
export default class ShareInstanceModal extends Vue {
  @Prop() uuid!: string;

  @Prop({ default: false }) open!: boolean;
  internalOpen = false;

  error: null | string = null;
  shareCode: null | string = null;
  loading = false;

  async shareInstance() {
    if (this.loading) {
      return;
    }

    this.loading = true;
    if (!(await validateAuthenticationOrSignIn()).ok) {
      this.close();
      return;
    }

    const shareRequest = await sendMessage("shareInstance", {
      uuid: this.uuid,
    }, 1_800_000);  // 30 minutes, if this isn't long enough, they need better internet...
    
    if (shareRequest.status === 'error') {
      this.error = shareRequest.message;
      console.log(this.error);
      return;
    }

    this.shareCode = shareRequest.code;
  }

  created() {
    this.internalOpen = this.open;
  }

  @Watch('open')
  onOpenChange(newVal: boolean) {
    this.internalOpen = newVal;
  }

  close() {
    this.loading = false;
    this.shareCode = null;
    this.error = null;
    this.$emit('closed');
  }

  get title() {
    if (this.error) {
      return ['Sharing failed!', 'Maybe give it another go?'];
    }

    if (this.loading) {
      return ['Uploading in progress', 'This might take a few minutes'];
    }

    return this.shareCode
      ? ['Upload complete!', "You're good to go!"]
      : ['Are you sure?', "You're about to share your instance"];
  }
}
</script>

<style scoped lang="scss"></style>
