<script lang="ts" setup>
import { onMounted, onUnmounted } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { useAds } from '@/composables/useAds.ts';

const {
  title,
  subTitle = '',
  hasCloser = true,
  open,
  size = 'small',
  permanent = false,
  closeOnBackgroundClick = true,
  externalContents = false,
} = defineProps<{
  title: string;
  subTitle?: string;
  hasCloser?: boolean;
  open: boolean;
  size?: ModalSizes;
  permanent?: boolean;
  closeOnBackgroundClick?: boolean;
  externalContents?: boolean;
}>()

const emit = defineEmits<{
  (e: 'closed'): void;
}>();

const ads = useAds();

onMounted(() => {
  document.addEventListener('keydown', onEsc)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onEsc)
})

function onEsc(event: any) {
  if (event.key !== 'Escape') {
    return;
  }

  close(true);
}

function close(background = false): void {
  if (!closeOnBackgroundClick && background) {
    return;
  }

  if (permanent) {
    return;
  }

  emit('closed');
}
</script>

<script lang="ts">
export type ModalSizes = 'small' | 'medium' | 'large';
</script>

<template>
  <Transition name="fade-and-grow">
    <div v-if="open" class="modal-container" :class="{ads: ads.adsEnabled}" @mousedown.self="() => close(true)">      
      <div class="modal-contents" :class="`${size}`">
        <div class="modal-header" :class="{'no-subtitle': !subTitle}">
          <div class="modal-heading">
            <div class="title">{{ title }}</div>
            <div class="subtitle" v-if="subTitle">{{ subTitle }}</div>
          </div>
          <div class="modal-closer" v-if="!permanent && hasCloser" @click="() => close(false)">
            <FontAwesomeIcon class="closer" :icon="faTimes" />
          </div>
        </div>
        <slot v-if="externalContents"></slot>
        <template v-else>
          <div class="modal-body">
            <slot></slot>
          </div>
          <div class="modal-footer" v-if="$slots.footer">
            <slot name="footer"></slot>
          </div>
        </template>
      </div>
    </div>
  </Transition>
</template>

<style lang="scss" scoped>
.modal-container {
  position: fixed;
  z-index: 49999; // just under of the title bar
  background: rgba(black, 0.85);
  width: 100vw;
  height: 100vh;
  top: 0;
  left: 0;
  display: grid;
  place-items: center;
  backdrop-filter: blur(5px);
  overflow: hidden;
  
  &.ads {
    width: calc(100vw - 400px);
  }
}

.fade-and-grow-enter-active,
.fade-and-grow-leave-active {
  transition: opacity 0.3s ease-in-out;

  .modal-contents {
    transition: opacity 0.3s ease-in-out, transform 0.3s ease-in-out;
  }
}

.fade-and-grow-enter,
.fade-and-grow-leave-to {
  opacity: 0;
  
  .modal-contents {
    opacity: 0;
    transform: scale(.97) translateY(50px);
  }
}
</style>
