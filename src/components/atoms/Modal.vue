<template>
  <Transition name="fade-and-grow">
    <div v-if="open" class="modal-container" @mousedown.self="close">
      <div class="modal-contents" :class="`${size}`">
        <div class="modal-header">
          <div class="modal-heading">
            <div class="title">{{ title }}</div>
            <div class="subtitle" v-if="subTitle">{{ subTitle }}</div>
          </div>
          <div class="modal-closer" v-if="!permanent">
            <font-awesome-icon class="closer" @click="close" icon="times" />
          </div>
        </div>
        <div class="modal-body">
          <slot></slot>
        </div>
        <div class="modal-footer" v-if="$slots.footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

export enum ModalSizes {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large',
}

@Component
export default class Modal extends Vue {
  @Prop() title!: string;
  @Prop({ default: '' }) subTitle!: string;
  @Prop({ default: true }) hasCloser!: boolean;

  @Prop() open!: boolean;
  @Prop({ default: ModalSizes.SMALL }) size!: ModalSizes;
  @Prop({ default: false }) private permanent!: boolean;

  public close(): void {
    if (this.permanent) {
      return;
    }

    this.$emit('closed');
  }
}
</script>

<style lang="scss" scoped>
.modal-container {
  position: fixed;
  z-index: 50001; // just ontop of the title bar
  background: rgba(black, 0.85);
  width: 100vw;
  height: 100vh;
  top: 0;
  left: 0;
  display: grid;
  place-items: center;
}

.modal-contents {
  max-height: 90vh;
  width: 550px;
  display: flex;
  flex-direction: column;
  background: #2a2a2a;
  border-radius: 5px;

  .modal-header {
    display: flex;
    justify-content: space-between;
    padding: 1rem 1.3rem;

    .modal-heading {
      flex: 1;

      .title {
        font-size: 1.3rem;
        font-weight: 900;
      }

      .subtitle {
        opacity: 0.8;
        font-size: 0.875rem;
      }
    }

    .modal-closer {
      opacity: 0.5;
      font-size: 1.5rem;
      padding: 0.5rem;
      transition: opacity 0.3s ease-in-out;
      cursor: pointer;

      &:hover {
        opacity: 1;
      }
    }
  }

  .modal-body {
    padding: 0 1rem 1rem 1.3rem;
    overflow-y: auto;
    height: 100%;
    margin-right: 7px;
    margin-bottom: 5px;
    font-size: 14px;

    &::-webkit-scrollbar {
      width: 6px;
      border-radius: 6px;
      z-index: 10;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
      z-index: 10;
      border-radius: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(black, 0.6);
      border-radius: 25px;
      z-index: 10;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #555;
    }
  }

  .modal-footer {
    background: #1f1f1f;
    border-radius: 0 0 5px 5px;
    padding: 0.8rem 1.3rem;
  }
}

.fade-and-grow-enter-active,
.fade-and-grow-leave-active {
  transition: opacity 0.3s ease-in-out, transform 0.3s ease-in-out;
}

.fade-and-grow-enter,
.fade-and-grow-leave-to {
  opacity: 0;
}
</style>
