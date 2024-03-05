<template>
  <Transition name="fade-and-grow">
    <div v-if="open" class="modal-container" :class="{ads: advertsEnabled}" @mousedown.self="() => close(true)">
      <div class="modal-contents" :class="`${size}`">
        <div class="modal-header" :class="{'no-subtitle': !subTitle}">
          <div class="modal-heading">
            <div class="title">{{ title }}</div>
            <div class="subtitle" v-if="subTitle">{{ subTitle }}</div>
          </div>
          <div class="modal-closer" v-if="!permanent && hasCloser" @click="() => close(false)">
            <font-awesome-icon class="closer" icon="times" />
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

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import Platform from '../../../utils/interface/electron-overwolf';
import {Getter, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import {ns} from '@/core/state/appState';
import {MineTogetherAccount} from '@/core/@types/javaApi';
import {adsEnabled} from '@/utils';

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
  @Prop({ default: false }) permanent!: boolean;

  @Prop({ default: true }) closeOnBackgroundClick!: boolean;
  @Prop({ default: false }) externalContents!: boolean;
  
  @State('settings') public settings!: SettingsState;
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;
  @Getter("getDebugDisabledAdAside", {namespace: 'core'}) private debugDisabledAdAside!: boolean
  
  get isOverwolf() {
    return Platform.isOverwolf();
  }

  mounted() {
    document.addEventListener('keydown', this.onEsc)
  }

  destroyed() {
    document.removeEventListener('keydown', this.onEsc)
  }

  onEsc(event: any) {
    if (event.key !== 'Escape') {
      return;
    }

    this.close(true);
  }
  
  public close(background = false): void {
    if (!this.closeOnBackgroundClick && background) {
      return;
    }

    if (this.permanent) {
      return;
    }

    this.$emit('closed');
  }
  
  get advertsEnabled(): boolean {
    return adsEnabled(this.settings.settings, this.getMtAccount, this.debugDisabledAdAside);
  }
}
</script>

<style lang="scss" scoped>
.modal-container {
  position: fixed;
  z-index: 49999; // just under of the title bar
  background: rgba(black, 0.85);
  width: 100vw;
  height: calc(100vh - 2px);
  top: 0;
  left: 0;
  display: grid;
  place-items: center;
  backdrop-filter: blur(5px);
  border-right: 1px solid rgba(white, .15);
  
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
