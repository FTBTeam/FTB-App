<template>
  <div class="">
    <router-view />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class SupportBase extends Vue {}
</script>

<style lang="scss" scoped></style>
