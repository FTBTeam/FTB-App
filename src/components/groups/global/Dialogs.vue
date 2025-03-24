<script lang="ts" setup>
import {adsEnabled, parseMarkdown} from '@/utils';
import UiButton from '@/components/ui/UiButton.vue';
import {SettingsState} from '@/modules/settings/types';
import { useAttachDomEvent } from '@/composables';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useDialogsStore } from '@/store/dialogStore.ts';

const dialogStore = useDialogsStore();

// TODO: [port] Fix me
// @State('settings') public settings!: SettingsState;
// @Getter("getDebugDisabledAdAside", {namespace: 'core'}) private debugDisabledAdAside!: boolean
const settings: SettingsState = {};
const debugDisabledAdAside: boolean = false;

useAttachDomEvent<KeyboardEvent>('keydown', (event) => {
  if (event.key !== 'Escape') {
    return;
  }

  closeTopDialog();
})

function closeTopDialog() {
  if (dialogStore.dialogs.length) {
    dialogStore.closeDialog(dialogStore.dialogs[dialogStore.dialogs.length - 1]);
  }
}

const advertsEnabled = adsEnabled(settings, debugDisabledAdAside);
</script>

<template>
  <transition name="transition-fade" :duration="250">
    <div class="dialog-container" :class="{ads: advertsEnabled}" v-if="dialogStore.dialogs.length" @click.self="closeTopDialog">
      <transition-group class="stacker" tag="div" name="transition-fade" :duration="250">
        <div
          v-for="(dialog, index) in dialogStore.dialogs"
          :key="`dialog-${index}`"
          class="dialog"
          :class="{[dialog.type as string]: true, 'active': index === dialogStore.dialogs.length - 1}"
        >
          <div class="modal-contents" :style="`
            z-index: ${(501 + (dialogStore.dialogs.length - 1) - ((dialogStore.dialogs.length - 1) - index))};
            transform: scale(${1 - (((dialogStore.dialogs.length - 1) - index) * .1)}) translateX(-${15 * ((dialogStore.dialogs.length - 1) - index)}px);
            opacity: ${1 - ((dialogStore.dialogs.length - 1) - index) * 0.2};
          `">
            <div class="modal-header" :class="{'no-subtitle': !dialog.subTitle}">
              <div class="modal-heading">
                <div class="title">{{ dialog.title }}</div>
                <div class="subtitle" v-if="dialog.subTitle">{{ dialog.subTitle }}</div>
              </div>
              <div class="modal-closer" @click="() => closeTopDialog()">
                <FontAwesomeIcon class="closer" icon="times" />
              </div>
            </div>
            
            <div class="modal-body wysiwyg" v-html="parseMarkdown(dialog.content)" />
              
            <div class="modal-footer">
              <div class="buttons">
                <UiButton v-for="(button, index) in dialog.buttons"
                            :working="dialog.working"
                            :key="index"
                            :type="button.type === 'error' ? 'danger' : button.type"
                            :icon="button.icon"
                            @click="button.action">
                    {{ button.text }}
                </UiButton>
              </div>
            </div>
          </div>
        </div>
      </transition-group>
    </div>
  </transition>
</template>

<style lang="scss" scoped>
.dialog-container {
  position: absolute;
  z-index: 200;
  backdrop-filter: blur(3px);
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: rgba(black, 0.75);
  
  &.ads {
    width: calc(100% - 400px);
  }
  
  .stacker {
    position: relative;
    z-index: 500;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .dialog {
    transition: opacity .25s ease-in-out;
    opacity: 1;
    position: absolute;
    
    .modal-contents {
      transform-origin: left center;
      transition: transform .25s ease-in-out, opacity .25s ease-in-out;
    }
    
    .buttons {
      display: flex;
      gap: .25rem .5rem;
      justify-content: flex-end;
      flex-wrap: wrap;
    }
  }
}
</style>