<template>
  <div class="global-components">
    <content-menu-global />
    
    <modal 
      v-if="modal" 
      :open="!!modal"
      @closed="hideModal" 
      :external-contents="true"
      :title="modal.title"
    >
      <modal-body>
        <div class="break-all overflow-auto" v-html="modal.message" />
      </modal-body>
      <modal-footer>
        <div class="flex justify-end gap-4">
          <ui-button v-for="(button, index) in modal.buttons" :key="index" @click="modalFeedback(button)" :type="button.type">
            {{ button.name }}            
          </ui-button>
        </div>
      </modal-footer>
    </modal>
        
    <LoginModal :open="getSignInOpened" @closed="() => closeSignIn()" />

    <!-- Only checks for an update once during startup -->
    <changelog />
    <dialogs />
    <alerts />
    <launch-instance-dialog />
    
    <dev-tools-actions />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Action, Getter, State} from 'vuex-class';
import Changelog from '@/components/templates/changelogs/Changelog.vue';
import Dialogs from '@/components/core/global/Dialogs.vue';
import Alerts from '@/components/core/global/Alerts.vue';
import {SocketState} from '@/modules/websocket/types';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import UiButton from '@/components/core/ui/UiButton.vue';
import {ModalButton, OpenModalData} from '@/core/@types/javaApi';
import DevToolsActions from '@/components/core/misc/DevToolsActions.vue';
import ContentMenuGlobal from '@/components/core/global/contextMenu/ContentMenuGlobal.vue';
import LoginModal from '@/components/core/auth/LoginModal.vue';
import LaunchInstanceDialog from '@/components/core/dialogs/LaunchInstanceDialog.vue';

@Component({
  components: {
    LaunchInstanceDialog,
    LoginModal,
    ContentMenuGlobal,
    DevToolsActions,
    UiButton,
    Alerts,
    Dialogs,
    Changelog
  },
})
export default class GlobalComponents extends Vue {
  @Action('hideModal') hideModal: any;

  @Getter('getSignInOpened', { namespace: 'core' }) getSignInOpened: any;
  @Action('closeSignIn', { namespace: 'core' }) closeSignIn: any;
  
  @State('websocket') websocket?: SocketState;
  
  get modal() {
    return this.websocket?.modal as OpenModalData | null | undefined;
  }

  public modalFeedback(button: ModalButton) {
    gobbleError(() => sendMessage("modalCallback", {
      id: this.modal?.id ?? "",
      message: button.message
    }))
  }
}
</script>

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
