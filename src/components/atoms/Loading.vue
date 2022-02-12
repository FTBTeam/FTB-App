<template>
  <div class="flex flex-1 flex-col justify-center items-center">
    <font-awesome-icon
      icon="circle-notch"
      spin
      :style="{ 'font-size': size + 'rem', color: 'rgba(255, 255, 255, 0.1)' }"
      class="mb-4"
    />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Loading extends Vue {
  @Prop({ default: '12' }) size!: string;
}
</script>

<style scoped></style>
