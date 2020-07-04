<template>
  <div :class="`ftb-button ${!noPadding ? '' : 'p-2'} ${color ? 'bg-' + color : ''} cursor-pointer ${isRounded ? 'rounded' : ''}`" @click="$emit('click')">
    <p class="cursor-pointer" :class="cssClass">
      <slot></slot>
    </p>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';

@Component({
    name: 'ftb-button',
    props: {
        click: Function,
        color: String,
        cssClass:  String,
        noPadding: Boolean,
        isRounded: {
          type: Boolean,
          default: true,
        },
    },
})
export default class FTBButton extends Vue {
}
</script>

<style lang="scss">
  .backbtn {
    position: absolute;
    background-color: rgba(255,255,255,0.2);
    border: 1px solid rgba(255,255,255,1);
    margin-top: 10px;
    /*margin-left: 7px;*/
    opacity: 0.6;
    width: 48px;
  }
  .backbtn:hover{
    opacity: 0.8;
  }
</style>
