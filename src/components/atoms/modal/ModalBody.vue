<template>
  <div class="modal-body">
    <slot></slot>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class ModalBody extends Vue {}
</script>

<style lang="scss" scoped>
.modal-body {
  padding: 0 1rem 1rem 1.3rem;
  overflow-y: auto;
  height: 100%;
  margin-right: 7px;
  margin-bottom: 5px;
  font-size: 14px;

  &::-webkit-scrollbar {
    width: 6px;
    border-radius: 6px;
    z-index: 10;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
    z-index: 10;
    border-radius: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(black, 0.6);
    border-radius: 25px;
    z-index: 10;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #555;
  }
}
</style>
