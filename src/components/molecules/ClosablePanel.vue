<template>
  <transition name="slide-in-out" duration="250">
    <div v-if="open" class="closable-panel" :class="{ overwolf: platform.isOverwolf(), 'is-mac': isMac, ads: advertsEnabled }" @click.self="close" >
      <div class="panel-container">
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
    </div>
  </transition>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import {Emit, Prop} from 'vue-property-decorator';
import Platform from '@/utils/interface/electron-overwolf';
import os from 'os';
import {Getter, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import {AuthState} from '@/modules/auth/types';

// TODO: (M#02) Remove this component and replace it with a modal that support full screen
@Component
export default class ClosablePanel extends Vue {
  @Prop() open!: boolean;
  @Prop() title!: string;
  @Prop({ default: '' }) subtitle!: string;
  @Prop({ default: true }) scrollable!: boolean;

  @Emit('close')
  close() {}

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
    
    this.close();
  }
  
  platform = Platform;
  
  // Apparently OS is safe on overwolf?
  isMac = os.type() === 'Darwin';

  // Not ideal...
  @State('settings') public settings!: SettingsState;
  @State('auth') public auth!: AuthState;
  @Getter("getDebugDisabledAdAside", {namespace: 'core'}) private debugDisabledAdAside!: boolean
  get advertsEnabled(): boolean {
    if (process.env.NODE_ENV !== "production" && this.debugDisabledAdAside) {
      return false
    }

    if (!this.auth?.token?.activePlan) {
      return true;
    }

    // If this fails, show the ads
    return (this.settings?.settings?.appearance?.showAds === true) ?? true;
  }
}
</script>

<style scoped lang="scss">
.closable-panel {
  position: fixed;
  height: calc(100% - 1.8rem);
  top: 2rem; // windows + linux title bar?
  left: 0;

  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  background-color: rgba(black, .6);
  backdrop-filter: blur(5px);
  width: 100vw;
  
  &.ads {
    width: calc(100% - 300px - (2.5rem));

    &.overwolf {
      width: calc(100% - 400px);
    }
  }

  &.is-mac {
    top: 1.8rem;
  }
  
  .panel-container {
    background-color: var(--color-navbar);
    width: 95%;
    height: 95%;
    padding: 1rem 0;
    
    display: flex;
    flex-direction: column;
    border-radius: 8px;

    > .heading {
      display: flex;
      justify-content: space-between;
      margin-bottom: 1rem;
      padding: 0 1.5rem;

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
      flex: 1;
      //max-height: 100%;
      overflow: auto;
      padding: 0 calc(1.5rem - .5rem) 2rem;
      margin: 0 .5rem;
    }
  }
  
  //position: fixed;
  //top: 1.5rem;
  //left: 1.5rem;
  //padding-top: 1.5rem;
  //width: calc((100% - 300px - (2.5rem)) - 3rem);
  //height: calc(100vh - 4rem);
  //border-radius: 10px;
  //z-index: 10000;
  //display: flex;
  //flex-direction: column;
  //background-color: #343434;
  //box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  //
  //&.overwolf {
  //  width: calc(100% - 400px);
  //}
  //
  //&.is-mac {
  //  top: calc(1.8rem + 1rem);
  //}
}

.slide-in-out-enter-active,
.slide-in-out-leave-active {
  transition: opacity 0.25s ease-in-out;
  
  .panel-container {
    transition: transform 0.25s cubic-bezier(.67,0,.2,1);
  }
}

.slide-in-out-enter,
.slide-in-out-leave-to {
  .panel-container {
    transform: translateY(30px);
  }
  
  opacity: 0;
}
</style>
