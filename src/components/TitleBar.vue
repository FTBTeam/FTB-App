<template>
  <div class="titlebar">
    <div class="icons" v-if="!isMac">
      <div class="title-action close" @click="close">
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12" class="pointer-events-none">
          <line x1="0.71" y1="11.12" x2="11.11" y2="0.72" stroke-width="2"/>
          <line x1="0.77" y1="0.71" x2="11.18" y2="11.12" stroke-width="2"/>
        </svg>
      </div>
      <div class="title-action" @click="max">
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12" class="pointer-events-none">
          <rect x="1" y="1" width="10" height="10" stroke-width="2"/>
        </svg>
      </div>
      <div class="title-action" @click="minimise">
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 1" class="pointer-events-none">
          <line y1="0.5" x2="11" y2="0.5" stroke-width="2"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
    import {Component, Prop, Vue} from 'vue-property-decorator';
    import {Action} from 'vuex-class';
    import {remote} from 'electron';
    import os from 'os';

    @Component
    export default class TitleBar extends Vue {
        @Action('sendMessage') public sendMessage: any;
        public isMac: boolean = false;

        public mounted() {
            if (os.type() === 'Darwin') {
                this.isMac = true;
            } else {
                this.isMac = false;
            }
        }

        // @ts-ignore
        public close(): void {
            remote.getCurrentWindow().close();
        }

        // @ts-ignore
        public minimise(): void {
            remote.getCurrentWindow().minimize();
        }

        // @ts-ignore
        public max(): void {
            if (!remote.getCurrentWindow().isMaximized()) {
                remote.getCurrentWindow().maximize();
            } else {
                remote.getCurrentWindow().unmaximize();
            }
        }
    }
</script>

<style scoped lang="scss">
  .titlebar {
    width: 100%;
    z-index: 10;
    height: 1.5rem;
    position: absolute;
    background-color: transparent;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-end;
    -webkit-app-region: drag;

    .icons {
      display: flex;
      flex-direction: row-reverse;
      background-color: #313131;
    }

    -webkit-user-select: none;
  }

  .title-action {
    -webkit-app-region: no-drag;
    cursor: pointer;
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 1.5rem;
    margin-right: 5px;
    padding: 0 10px;
    fill: none;
    stroke: #989898;

    &:hover {
      background-color: #414141;
    }

    &.close {
      margin-right: 0;
    }

    &.close:hover {
      stroke: #fff;
      background-color: #fc3636;
    }
  }
</style>
