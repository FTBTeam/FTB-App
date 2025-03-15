<script lang="ts" setup>
import Changelog from '@/components/groups/changelogs/Changelog.vue';
import Dialogs from '@/components/groups/global/Dialogs.vue';
import Alerts from '@/components/groups/global/Alerts.vue';
import {SocketState} from '@/modules/websocket/types';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import UiButton from '@/components/ui/UiButton.vue';
import {ModalButton, OpenModalData} from '@/core/@types/javaApi';
import DevToolsActions from '@/components/layout/DevToolsActions.vue';
import ContentMenuGlobal from '@/components/groups/global/contextMenu/ContentMenuGlobal.vue';
import LoginModal from '@/components/groups/auth/LoginModal.vue';
import LaunchInstanceDialog from '@/components/modals/LaunchInstanceDialog.vue';
import { computed } from 'vue';
import { Modal, ModalFooter } from '@/components/ui'

// TODO: [port] fixme
// @Action('hideModal') hideModal: any;
// @Getter('getSignInOpened', { namespace: 'core' }) getSignInOpened: any;
// @Action('closeSignIn', { namespace: 'core' }) closeSignIn: any;
// @State('websocket') websocket?: SocketState;

function hideModal() {}
const getSignInOpened = false;
function closeSignIn() {}

const websocket: SocketState = null;

const activeModal = computed(() => {
  return websocket?.modal as OpenModalData | null | undefined;
})

function modalFeedback(button: ModalButton) {
  gobbleError(() => sendMessage("modalCallback", {
    id: this.modal?.id ?? "",
    message: button.message
  }))
}
</script>

<template>
  <div class="global-components">
    <content-menu-global />
    
    <Modal 
      v-if="activeModal" 
      :open="!!activeModal"
      @closed="hideModal" 
      :external-contents="true"
      :title="activeModal.title"
      style="z-index: 50000"
    >
      <ModalButton>
        <div class="break-all overflow-auto" v-html="activeModal.message" />
      </ModalButton>
      <ModalFooter>
        <div class="flex justify-end gap-4">
          <UiButton v-for="(button, index) in activeModal.buttons" :key="index" @click="modalFeedback(button)" :type="button.type">
            {{ button.name }}            
          </UiButton>
        </div>
      </ModalFooter>
    </Modal>
        
    <LoginModal :open="getSignInOpened" @closed="() => closeSignIn()" />

    <!-- Only checks for an update once during startup -->
    <changelog />
    <dialogs />
    <alerts />
    <launch-instance-dialog />
    
    <dev-tools-actions />
  </div>
</template>

<style lang="scss" scoped>
.alerts {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 5000;

  display: flex;
  flex-direction: column;
  align-items: flex-end;

  .alert {
    display: flex;
    align-items: center;
    padding: 0.5rem 1rem;
    margin-top: 0.5rem;
    border-radius: 5px;

    span {
      margin-right: 0.5rem;
    }

    .message {
      margin-right: 0.5rem;
    }

    .close {
      cursor: pointer;
      padding: 0 0.5rem;
    }
  }
}
</style>
