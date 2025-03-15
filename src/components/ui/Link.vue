<template>
  <a class="link hover:text-blue-500 text-blue-300 hover:underline italic cursor-pointer" @click.prevent="open">
    <slot></slot>
  </a>
</template>

<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';

const props = defineProps<{
  url: string;
}>()

function open(e: any) {
  e.preventDefault();
  platform.get.utils.openUrl(props.url);
}
</script>