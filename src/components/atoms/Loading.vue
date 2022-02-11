<template>
  <div class="flex flex-1 flex-col justify-center items-center pt-10">
      <font-awesome-icon icon="sync-alt" spin style="font-size: 30vh; color: rgba(0,0,0,0.1);" class="mb-4"/>
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