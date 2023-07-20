<template>
  <Transition name="fade">
    <div
      v-if="visible"
      class="left-0 top-0 flex items-center justify-center bg-transparent-black h-screen ftb-modal"
      :class="{os: isOverwolf}"
      @mousedown.self="hide"
    >
      <div
        class="bg-navbar rounded shadow px-4 m-4 py-4 max-h-full text-white pointer-events-auto ftb-modal-contents"
        :class="`${size}`"
      >
        <slot></slot>
      </div>
    </div>
  </Transition>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import Platform from '@/utils/interface/electron-overwolf';

export enum ModalSizes {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large',
  LARGE_DYNAMIC = 'large-dynamic',
}

@Component
export default class FTBModal extends Vue {
  @Prop() visible!: boolean;
  @Prop({ default: ModalSizes.SMALL }) size!: ModalSizes;
  @Prop({ default: true }) private dismissable!: boolean;
  @Prop({ default: false }) isLarge!: boolean;

  public hide(): void {
    if (this.dismissable) {
      this.$emit('dismiss-modal');
    }
  }
  
  get isOverwolf() {
    return Platform.isOverwolf();
  }
}
</script>

<style lang="scss" scoped>
.ftb-modal {
  position: fixed;
  z-index: 500;
  background: rgba(black, 0.85);
  width: calc(100% - (300px + 2.5rem));
  
  &.ow {
    width: calc(100vw - 400px);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.ftb-modal-contents {
  max-height: 80%;

  &.small {
    width: 450px;
  }

  &.medium {
    width: 550px;
  }

  &.large {
    width: 95%;
    height: 80%;
  }

  &.large-dynamic {
    width: 95%;
    max-width: 700px;
  }
}
</style>
