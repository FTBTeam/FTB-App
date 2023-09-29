<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Share code" sub-title="Create an instance from a share code">
    <modal-body>
      <p class="mb-4">If you have a share code from a friend, you can use that here to import a new instance.</p>
      <ftb-input
        placeholder="share code"
        label="Share code"
        v-model="shareCode"
        class="mb-4 text-base"
      />
    </modal-body>
    <modal-footer>
      <ui-button icon="download" type="primary" @click="checkAndInstall">
        Install
      </ui-button>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';

import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import Modal from '@/components/atoms/modal/Modal.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {RouterNames} from '@/router';
import {safeNavigate} from '@/utils';

@Component({
  components: {UiButton, Modal, ModalBody}
})
export default class ShareCodeInstance extends Vue {
  @Prop() open!: boolean;
  @Emit() close() {}

  shareCode = ""

  async checkAndInstall() {
    if (this.shareCode === '') {
      return;
    }

    const checkResult = await sendMessage('checkShareCode', { shareCode: this.shareCode });
    
    if (!checkResult.success) {
      alertController.error(`Unable to find a valid pack with the code of ${this.shareCode} `)
      return;
    }
    
    await instanceInstallController.requestShareImport(this.shareCode);
    this.shareCode = "";

    await safeNavigate(RouterNames.ROOT_LIBRARY)
    
    this.close();
  }
}
</script>

<style lang="scss" scoped>

</style>