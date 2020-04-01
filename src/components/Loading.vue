<template>
  <div class="flex items-center pt-10">
      <h1 class="text-4xl">Loading...</h1>
      <font-awesome-icon icon="spinner" spin size="2x" />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Loading extends Vue {
}
</script>

<style scoped>

</style>