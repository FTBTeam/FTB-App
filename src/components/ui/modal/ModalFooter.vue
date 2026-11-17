<template>
  <div class="modal-footer">
    <slot></slot>
  </div>
</template>

<style lang="scss" scoped>
.modal-footer {
  background: #1f1f1f;
  border-radius: 0 0 var(--border-radius) var(--border-radius);
  padding: 0.8rem 1.3rem;
}
</style>
