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
    <authentication v-if="getSignInOpened.open" @close="closeSignIn()" />

    <!-- Instance Loading -->
    <instance-loading/>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import FTBModal from '@/components/atoms/FTBModal.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { Action, Getter } from 'vuex-class';
import Authentication from '@/components/templates/authentication/Authentication.vue';
import InstanceLoading from '@/components/templates/loading/InstanceLoading.vue';

@Component({
  components: {
    Authentication,
    InstanceLoading,
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

<style lang="scss" scoped></style>
