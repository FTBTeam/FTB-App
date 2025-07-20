<script lang="ts" setup>
import { ref, watch } from 'vue'

type ProgressType = "primary" | "muted"

const {
  progress = 0,
  infinite = false,
  doProgressAnimation = true,
  type = 'primary',
  inverted = false,
} = defineProps<{
  progress?: number;
  infinite?: boolean;
  doProgressAnimation?: boolean;
  type?: ProgressType;
  inverted?: boolean;
}>()

const isResetting = ref(false);

watch(() => progress, (newValue, oldValue) => {
  if (newValue === oldValue || isResetting.value) {
    return;
  }
  
  // Is the new progress less than the old one?
  if (newValue < oldValue) {
    isResetting.value = true;
    setTimeout(() => {
      isResetting.value = false;
    }, 100);
  }
});
</script>

<template>
  <div class="progress" :class="{'infinite': infinite, 'animated': doProgressAnimation, 'inverted': inverted, [type]: true, 'resetting': isResetting}">
    <div class="bar" :style="{
      width: inverted ? `100%` : `${progress * 100}%`,
      marginLeft: inverted ? `${100 - progress * 100}%` : '0',
    }"></div>
  </div>
</template>

<style lang="scss" scoped>
@import 'tailwindcss/theme' theme(reference);

.progress {
  width: 100%;
  height: 10px;
  background: #151515;
  border-radius: 10px;
  overflow: hidden;
  position: relative;

  &:not(.resetting) .bar {
    transition: width .15s ease-in-out, margin-left 0.1s ease-in-out;
  }
  
  .bar {
    height: 100%;
    background: var(--color-green-600);
  }
  
  &.muted .bar {
    background: var(--color-gray-700);
  }

  &.animated::after {
    content: '';
    top: 0;
    width: 100%;
    height: 100%;
    background: white;
    position: absolute;
    left: -100%;

    animation: leftToRight 3s ease-in-out infinite;

    @keyframes leftToRight {
      0% {
        opacity: 0.08;
        left: -100%;
      }
      50% {
        opacity: 0.15;
        left: 0;
      }
      100% {
        opacity: 0.08;
        left: 100%;
      }
    }
  }

  &.animated.inverted::after {
    animation: rightToLeft 3s ease-in-out infinite;
    @keyframes rightToLeft {
      0% {
        opacity: 0.08;
        left: 100%;
      }
      50% {
        opacity: 0.15;
        left: 0;
      }
      100% {
        opacity: 0.08;
        left: -100%;
      }
    }
  }

  &.infinite::after {
    background-color: var(--color-green-600);
    animation: leftToRight2 3s ease-in-out infinite;

    @keyframes leftToRight2 {
      0% {
        opacity: .8;
        left: -100%;
      }
      50% {
        opacity: 1;
        left: 0;
      }
      100% {
        opacity: .8;
        left: 100%;
      }
    }
  }
}
</style>
