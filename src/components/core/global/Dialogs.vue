<template>
  <transition name="transition-fade" duration="250">
    <div class="dialog-container" :class="{'overwolf': appPlatform.isOverwolf()}" v-if="dialogs.length" @click.self="closeTopDialog">
      <transition-group class="stacker" tag="div" name="transition-fade" duration="250">
        <div
          v-for="(dialog, index) in dialogs"
          :key="`dialog-${index}`"
          class="dialog"
          :class="{[dialog.type]: true, 'active': index === dialogs.length - 1}"
        >
          <div class="modal-contents" :style="`
            z-index: ${(501 + (dialogs.length - 1) - ((dialogs.length - 1) - index))};
            transform: scale(${1 - (((dialogs.length - 1) - index) * .1)}) translateX(-${15 * ((dialogs.length - 1) - index)}px);
            opacity: ${1 - ((dialogs.length - 1) - index) * 0.2};
          `">
            <div class="modal-header" :class="{'no-subtitle': !dialog.subTitle}">
              <div class="modal-heading">
                <div class="title">{{ dialog.title }}</div>
                <div class="subtitle" v-if="dialog.subTitle">{{ dialog.subTitle }}</div>
              </div>
              <div class="modal-closer" @click="() => closeTopDialog()">
                <font-awesome-icon class="closer" icon="times" />
              </div>
            </div>
            
            <div class="modal-body wysiwyg" v-html="parseMarkdown(dialog.content)" />
              
            <div class="modal-footer">
              <div class="buttons">
                <ftb-button v-for="(button, index) in dialog.buttons"
                            :disabled="dialog.working"
                            :key="index"
                            :color="button.type === 'error' ? 'danger' : button.type"
                            class="px-4 py-2"
                            @click="button.action">
                  <div class="btn-content flex items-center gap-4">
                    <font-awesome-icon v-if="button.icon" :icon="button.icon"/>
                    {{ button.text }}
                  </div>
                </ftb-button>
              </div>
            </div>
          </div>
        </div>
      </transition-group>
    </div>
  </transition>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Dialog} from '@/core/state/misc/dialogsState';
import {ns} from '@/core/state/appState';
import {Action, Getter } from 'vuex-class';
import {parseMarkdown} from '@/utils';

@Component
export default class Dialogs extends Vue {
  @Getter("dialogs", ns("v2/dialogs")) dialogs!: Dialog[];
  @Action("closeDialog", ns("v2/dialogs")) closeDialog!: (dialog: Dialog) => void;

  parseMarkdown = parseMarkdown
  
  closeTopDialog() {
    if (this.dialogs.length) {
      this.closeDialog(this.dialogs[this.dialogs.length - 1]);
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-container {
  position: absolute;
  z-index: 200;
  backdrop-filter: blur(3px);
  top: 0;
  left: 0;
  width: calc(100% - (300px + 2.5rem));
  height: 100%;
  overflow: hidden;
  background: rgba(black, 0.75);
  
  &.overwolf {
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