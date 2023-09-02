<template>
  <div>
    
  </div>
</template>

<script lang="ts">
import {Prop, Component} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';

@Component
export default class PackPreview extends PackCardCommon {
  @Prop() packId!: number;
}
</script>

<style lang="scss" scoped>

</style>