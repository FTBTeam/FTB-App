<template>
  <div class="settings-sidebar">
    <div class="back">
      <font-awesome-icon icon="chevron-left" class="mr-2" />
      Back to app
    </div>
    <nav>
      <div class="item active">
        <font-awesome-icon icon="gamepad" />
        <span>Instances</span>
      </div>
      <div class="item">
        <font-awesome-icon icon="cloud-download-alt" />
        <span>Downloads</span>
      </div>
      <div class="item">
        <font-awesome-icon icon="rocket" />
        <span>App</span>
      </div>
      <div class="item">
        <font-awesome-icon icon="info" />
        <span>App info</span>
      </div>
    </nav>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class SettingsSidebar extends Vue {}
</script>

<style scoped lang="scss">
.settings-sidebar {
  background-color: var(--color-navbar);
  width: 300px;
  padding: 2rem 1.5rem;

  nav {
    margin-top: 2rem;

    .item {
      padding: 1rem;
      margin-bottom: 0.5rem;
      border-radius: 5px;
      display: flex;
      align-items: center;
      cursor: pointer;

      &.active {
        background: green;
      }

      svg {
        margin-right: 0.5rem;
        width: 30px;
        font-size: 18px;
      }
    }
  }
}
</style>
