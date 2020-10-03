<template>
  <div :class="`ftb-button ${!noPadding ? '' : 'p-2'} ${color ? 'bg-' + color : ''} ${color ? 'hover:bg-light-' + color : ''} ${disabled ? 'cursor-not-allowed disabled' : 'cursor-pointer'} ${isRounded ? 'rounded' : ''}`" @click="handleClick">
    <p :class="`${cssClass} ${disabled ? 'cursor-not-allowed' : 'cursor-pointer'}`">
      <slot></slot>
    </p>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';

@Component({
    name: 'ftb-button',
    props: {
        disabled: Boolean,
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
  @Prop({default: false})
  private disabled!: boolean;
  private handleClick(){
    if(this.disabled){
      return;
    }
    this.$emit('click')
  }
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
  .ftb-button svg {
    cursor: pointer;
  }
  .disabled {
    filter: grayscale(0.8) brightness(0.5);
  }
</style>
