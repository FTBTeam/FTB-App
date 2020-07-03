<template>
  <Transition name="fade">
  <div
    v-if="visible"
    class="left-0 top-0 fixed flex items-center justify-center bg-transparent-black h-screen w-full z-10 "
    v-on:click.self="hide">
    <div
      class="bg-navbar rounded shadow px-8 m-4 py-4 max-w-lg max-h-full text-white pointer-events-auto min-w-1/4">
      <slot></slot>
    </div>
  </div>
  </Transition>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    name: 'FTBModal',
    props: [
        'visible',
    ],
})
export default class FTBModal extends Vue {
    @Prop()
    private visible!: boolean;

    public hide(): void {
      this.$emit('dismiss-modal');
    }
}
</script>
<style>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>