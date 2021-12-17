<template>
  <div class="progress">
    <div class="bar" v-bind:style="{ width: `${progress * 100}%`, transition: 'width 0.5s ease' }"></div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component({
  props: {
    progress: Number,
  },
})
export default class ProgressBar extends Vue {}
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
    height: 100%;
    background: var(--color-primary-button);
  }
}
</style>
