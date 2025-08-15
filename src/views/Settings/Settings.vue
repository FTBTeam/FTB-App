<script lang="ts" setup>
import SettingsSidebar from '@/views/Settings/SettingsSidebar.vue';

</script>

<template>
  <div class="settings">
    <SettingsSidebar />
    <main>
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.settings {
  height: 100%;
  display: flex;

  > main {
    flex: 1;
    padding: 1.5rem;
    height: 100%;
    overflow-y: auto;
    background-color: var(--color-navbar);
  }
}
</style>
