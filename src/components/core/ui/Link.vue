<template>
  <a class="link hover:text-blue-500 underline italic" @click.prevent="open">
    <slot></slot>
  </a>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class Link extends Vue {
  @Prop() url!: string;
  platform = platform;
  
  open(e: any) {
    e.preventDefault();
    platform.get.utils.openUrl(this.url);
  }
}
</script>

<style lang="scss" scoped>

</style>