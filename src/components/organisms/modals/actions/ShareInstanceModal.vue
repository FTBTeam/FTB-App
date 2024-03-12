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
    <div class="code" v-else-if="shareCode">
      <p class="mb-4">Provide this code to anyone you would like to share this Modpack with. They can use this code to import this Modpack to their own app.</p>
      
      <div class="share-code font-mono select-text mb-8">
        <span v-for="(char, index) in shareCode" :key="index">{{char}}</span>
      </div>

      <ui-button type="success" size="large" :full-width="true" icon="copy" @click="copy">Copy</ui-button>
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
import UiButton from '@/components/core/ui/UiButton.vue';
import platform from '@/utils/interface/electron-overwolf';
import {alertController} from '@/core/controllers/alertController';

@Component({
  components: {UiButton, ProgressBar },
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
      this.loading = false;
      return;
    }

    this.shareCode = shareRequest.code;
    this.loading = false;
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

  copy() {
    if (!this.shareCode) {
      return;
    }

    platform.get.cb.copy(this.shareCode);
    alertController.success('Copied to clipboard!');
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

<style scoped lang="scss">
.share-code {
  display: flex;
  justify-content: space-between;
  font-size: 5rem;
  padding: 0 1rem;
  gap: 1rem;
  
  span {
    display: inline-block;
    border-bottom: 2px dashed rgba(white, .2);
  }
}
</style>
