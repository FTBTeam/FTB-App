<template>
  <div class="loading-two">
    <div class="loading-element">
      <div class="dot" />
      <div class="dot" />
      <div class="dot" />
    </div>
    <div class="title">{{ title }}</div>
    <div v-if="subTitle" class="subtitle">{{ subTitle }}</div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import { Prop, Vue } from 'vue-property-decorator';

@Component
export default class Loader extends Vue {
  @Prop({ default: 'Loading' }) title!: string;
  @Prop({ default: "Shouldn't be a moment" }) subTitle!: string | null;
}
</script>

<style lang="scss" scoped>
.loading-two {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 2.5rem 0 2rem 0;

  .loading-element {
    position: relative;
    font-size: 3rem;
    margin-bottom: 1.5rem;
    display: flex;
    gap: 1rem;

    height: 50px;

    .dot {
      background-color: #0787c1;
      width: 8px;
      height: inherit;

      opacity: 0;
      transform: scale(0);

      border-radius: 100px;
      animation: fade-and-scale infinite ease-in-out 2s;

      &:nth-child(2) {
        animation-delay: 0.5s;
        background-color: #798b2f;
      }

      &:nth-child(3) {
        animation-delay: 1s;
        background-color: #e65014;
      }

      @keyframes fade-and-scale {
        0%,
        100% {
          opacity: 0;
          transform: scale(0);
        }
        50% {
          opacity: 1;
          transform: scale(1);
        }
      }
    }
  }

  .title {
    font-weight: bold;
    font-size: 1.15rem;
    margin-bottom: 0.2rem;
  }

  .subtitle {
    font-style: italic;
    opacity: 0.8;
  }
}
</style>
