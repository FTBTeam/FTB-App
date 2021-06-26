<template>
  <div class="modpack-square">
    <div class="logo"></div>
    <div class="name"></div>
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
