<template>
  <div class="modal-footer">
    <slot></slot>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class ModalFooter extends Vue {}
</script>

<style lang="scss" scoped>
.modal-footer {
  background: #1f1f1f;
  border-radius: 0 0 5px 5px;
  padding: 0.8rem 1.3rem;
}
</style>
