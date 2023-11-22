<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Import" sub-title="Import instances from other launchers">
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
export default class ImportInstance extends Vue {
  @Prop() open!: boolean;
  @Emit() close() {}
}
</script>

<style lang="scss" scoped>

</style>