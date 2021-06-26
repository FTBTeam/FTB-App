<template>
  <div class="modpack">
    <div class="logo">hi</div>
    <div class="main"></div>
    <div class="right"></div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { Modpack } from "@/v2/types";

@Component
export default class ModpackCard extends Vue {
  @Prop() pack!: Modpack;
}
</script>

<style lang="scss" scoped>
.modpack {
  padding: 1rem;
  display: flex;
  justify-content: space-between;

  .logo {
  }

  .main {
  }

  .right {
  }
}
</style>
