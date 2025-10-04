<script lang="ts" setup>
import { useAttachDomEvent } from '@/composables';
import { useDialogsStore } from '@/store/dialogStore.ts';
import { useAds } from '@/composables/useAds.ts';
import Dialog from "@/components/groups/global/dialog/Dialog.vue";

const dialogStore = useDialogsStore();
const ads = useAds()

useAttachDomEvent<KeyboardEvent>('keydown', (event) => {
  if (event.key !== 'Escape') {
    return;
  }

  closeTopDialog();
})

function closeTopDialog() {
  if (dialogStore.dialogs.length) {
    dialogStore.closeDialog(dialogStore.dialogs[dialogStore.dialogs.length - 1]);
  }
}
</script>

<template>
  <transition name="transition-fade" :duration="250">
    <div class="dialog-container" :class="{ads: ads.adsEnabled.value}" v-if="dialogStore.dialogs.length" @click.self="closeTopDialog">
      <transition-group class="stacker" tag="div" name="transition-fade" :duration="250">
        <Dialog
          v-for="(dialog, index) in dialogStore.dialogs"
          :key="`dialog-${index}`"
          class="dialog"
          :class="{[dialog.type as string]: true, 'active': index === dialogStore.dialogs.length - 1}"
          :dialog="dialog"
          :depth="dialogStore.dialogs.length - 1 - index"
          @close="() => closeTopDialog()"
        />
      </transition-group>
    </div>
  </transition>
</template>

<style lang="scss" scoped>
.dialog-container {
  position: absolute;
  z-index: 200;
  backdrop-filter: blur(3px);
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: rgba(black, 0.75);
  
  &.ads {
    width: calc(100% - 400px);
  }
  
  .stacker {
    position: relative;
    z-index: 500;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>