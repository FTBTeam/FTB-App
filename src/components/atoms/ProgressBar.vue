<template>
  <div class="progress">
    <div class="bar"></div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class ProgressBar extends Vue {
}
</script>

<style lang="scss" scoped>
.progress {
  width: 100%;
  height: 10px;
  background: rgba(gray, 0.2);
  border-radius: 10px;
  overflow: hidden;
  position: relative;

  .bar {
    width: 100%;
    height: 100%;
    background: var(--color-primary-button);
    position: absolute;
    left: -100%;

    animation: leftToRight 1.5s ease-in-out infinite;

    @keyframes leftToRight {
      0% {
        left: -100%;
      }
      50% {
        left: 0;
      }
      100% {
        left: 100%;
      }
    }
  }
}
</style>