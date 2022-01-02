<template>
  <div class="progress">
    <div class="bar" :style="{ width: `${progress * 100}%`, transition: 'width 0.5s ease' }"></div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class ProgressBar extends Vue {
  @Prop({ default: 0 }) progress!: number;
}
</script>

<style lang="scss" scoped>
.progress {
  width: 100%;
  height: 10px;
  background: #151515;
  border-radius: 10px;
  overflow: hidden;
  position: relative;

  .bar {
    height: 100%;
    background: var(--color-primary-button);
  }

  &::after {
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
        opacity: 0.1;
        left: -100%;
      }
      50% {
        opacity: 0.2;
        left: 0;
      }
      100% {
        opacity: 0.1;
        left: 100%;
      }
    }
  }
}
</style>
