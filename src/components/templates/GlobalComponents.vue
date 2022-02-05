<template>
  <div class="global-components">
    <!-- Modals -->
    <FTBModal
      v-if="$store.state.websocket.modal !== undefined && $store.state.websocket.modal !== null"
      :visible="$store.state.websocket.modal !== null"
      @dismiss-modal="hideModal"
      :dismissable="$store.state.websocket.modal.dismissable"
    >
      <message-modal
        :title="$store.state.websocket.modal.title"
        :content="$store.state.websocket.modal.message"
        type="custom"
        :buttons="$store.state.websocket.modal.buttons"
        :modalID="$store.state.websocket.modal.id"
      />
    </FTBModal>

    <!-- Alerts -->
    <div class="alerts" v-if="$store.state.alerts">
      <div class="alert" v-for="(alert, index) of $store.state.alerts" :key="index" :class="`bg-${alert.type}`">
        <div class="message">
          <span class="font-bold">{{ alert.title }}</span>
          <div class="message">{{ alert.message }}</div>
        </div>

        <div class="close" @click="() => hideAlert(alert)"><font-awesome-icon icon="times" /></div>
      </div>
    </div>

    <!-- Authentication -->
    <authentication
      v-if="getSignInOpened.open"
      :jump="getSignInOpened.jumpToAuth"
      :uuid="getSignInOpened.uuid"
      :backAction="getSignInOpened.afterAction"
      @close="closeSignIn()"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import FTBModal from '@/components/atoms/FTBModal.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { Action, Getter } from 'vuex-class';
import Authentication from '@/components/templates/authentication/Authentication.vue';

@Component({
  components: {
    Authentication,
    FTBModal,
    MessageModal,
  },
})
export default class GlobalComponents extends Vue {
  @Action('hideModal') public hideModal: any;
  @Action('hideAlert') public hideAlert: any;

  @Getter('getSignInOpened', { namespace: 'core' }) public getSignInOpened: any;
  @Action('closeSignIn', { namespace: 'core' }) public closeSignIn: any;
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
