<template>
  <Transition name="fade">
    <div
      v-if="visible"
      class="left-0 top-0 flex items-center justify-center bg-transparent-black h-screen w-full ftb-modal"
      @mousedown.self="hide"
    >
      <div
        class="bg-navbar rounded shadow px-8 m-4 py-4 max-h-full text-white pointer-events-auto"
        :class="{ 'max-w-lg min-w-1/4': !isLarge, 'is-large': isLarge }"
      >
        <slot></slot>
      </div>
    </div>
  </Transition>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FTBModal extends Vue {
  @Prop() visible!: boolean;
  @Prop({ default: true }) private dismissable!: boolean;
  @Prop({ default: false }) isLarge!: boolean;

  public hide(): void {
    if (this.dismissable) {
      this.$emit('dismiss-modal');
    }
  }
}
</script>

<style lang="scss" scoped>
.ftb-modal {
  position: fixed;
  z-index: 500;
  background: rgba(black, 0.85);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.is-large {
  width: 80%;
  height: 80%;
}
</style>
