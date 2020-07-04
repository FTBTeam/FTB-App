<template>
  <div :class="`ftb-button p-2 bg-${color} cursor-pointer ${isRounded ? 'rounded' : ''}`" @click="$emit('click')">
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
</style>
