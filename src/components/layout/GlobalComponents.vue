<script lang="ts" setup>
import Changelog from '@/components/groups/changelogs/Changelog.vue';
import Dialogs from '@/components/groups/global/Dialogs.vue';
import Alerts from '@/components/groups/global/Alerts.vue';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import UiButton from '@/components/ui/UiButton.vue';
import DevToolsActions from '@/components/layout/DevToolsActions.vue';
import ContentMenuGlobal from '@/components/groups/global/contextMenu/ContentMenuGlobal.vue';
import MicrosoftLoginModal from '@/components/groups/auth/MicrosoftLoginModal.vue';
import LaunchInstanceDialog from '@/components/modals/LaunchInstanceDialog.vue';
import { Modal, ModalBody, ModalFooter } from '@/components/ui';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { useModalStore } from '@/store/modalStore.ts';
import { ModalButton } from '@/core/types/javaApi';
import FtbLoginModal from "@/components/groups/auth/FtbLoginModal.vue";
import ModpackPreview from "@/components/groups/global/modpackPreview/ModpackPreview.vue";
import ImagePreview from "@/components/groups/global/imagePreview/ImagePreview.vue";

const modalStore = useModalStore();
const accountStore = useAccountsStore();

function modalFeedback(button: ModalButton) {
  gobbleError(async () => {
    if (!modalStore.modal) {
      return;
    }

    await sendMessage("modalCallback", {
      id: modalStore.modal.id,
      message: button.message
    })
  })
}
</script>

<template>
  <div class="global-components">
    <ModpackPreview />
    <content-menu-global />
    <ImagePreview />
    
    <Modal 
      v-if="modalStore.modal" 
      :open="!!modalStore.modal"
      @closed="() => modalStore.closeModal()" 
      :external-contents="true"
      :title="modalStore.modal.title"
      style="z-index: 50000"
    >
      <ModalBody>
        <div class="break-all overflow-auto" v-html="modalStore.modal.message" />
      </ModalBody>
      <ModalFooter>
        <div class="flex justify-end gap-4">
          <UiButton v-for="(button, index) in modalStore.modal.buttons" :key="index" @click="modalFeedback(button)" :type="button.type as any">
            {{ button.name }}            
          </UiButton>
        </div>
      </ModalFooter>
    </Modal>
    
    <MicrosoftLoginModal :open="accountStore.signInOpen" @closed="() => accountStore.openSignIn(false)" />
    <FtbLoginModal :open="accountStore.signInFtbOpen" @closed="() => accountStore.openSignInFtb(false)" />

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
