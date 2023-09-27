<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Share code" sub-title="Create an instance from a share code">
    <modal-body>
      Hello
    </modal-body>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';

import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import Modal from '@/components/atoms/modal/Modal.vue';

@Component({
  components: {Modal, ModalBody}
})
export default class ShareCodeInstance extends Vue {
  @Prop() open!: boolean;
  @Emit() close() {}
}
</script>

<style lang="scss" scoped>

</style>