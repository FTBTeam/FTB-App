<template>
  <transition name="slide-in-out" duration="250">
    <div v-if="open" class="closable-panel" :class="{ overwolf: platform.isOverwolf() }">
      <div class="heading">
        <div class="main">
          <div class="title">{{ title }}</div>
          <div class="sub-title">{{ subtitle }}</div>
        </div>
        <div class="closer" @click="close">
          Close
          <font-awesome-icon icon="times" />
        </div>
      </div>
      <div class="content" :class="{ scrollable }">
        <slot />
      </div>
    </div>
  </transition>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import { Emit, Prop } from 'vue-property-decorator';
import Platform from '@/utils/interface/electron-overwolf';

@Component
export default class ClosablePanel extends Vue {
  @Prop() open!: boolean;
  @Prop() title!: string;
  @Prop({ default: '' }) subtitle!: string;
  @Prop({ default: true }) scrollable!: boolean;

  @Emit('close')
  close() {}

  platform = Platform;
}
</script>

<style lang="scss">
.closable-panel {
  position: fixed;
  top: 0;
  left: 0;
  padding-top: 3rem;
  width: calc(100% - 300px - (1.5rem * 2));
  height: 100%;
  z-index: 10000;
  background-color: #2e3031;

  &.overwolf {
    width: calc(100% - 300px);
  }

  > .heading {
    display: flex;
    justify-content: space-between;
    margin-bottom: 1rem;
    padding: 0 2rem;

    .main {
      flex: 1;
      padding-right: 1rem;

      .title {
        font-size: 1.3rem;
        font-weight: 900;
        margin-bottom: 0.3rem;
      }

      .sub-title {
        opacity: 0.8;
        font-size: 0.875rem;
        font-weight: bold;
      }
    }

    .closer {
      cursor: pointer;
      padding-left: 2rem;
      display: flex;
      align-items: center;

      svg {
        margin-left: 0.5rem;
      }
    }
  }

  > .content {
    max-height: 100%;
    overflow: auto;
    padding: 0 2rem 2rem;
  }
}

.slide-in-out-enter-active,
.slide-in-out-leave-active {
  transition: transform 0.25s ease-in-out, opacity 0.25s ease-in-out;
}

.slide-in-out-enter,
.slide-in-out-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}
</style>
