<template>
  <div class="settings">
    <settings-sidebar />
    <main>
      <router-view />
    </main>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import SettingsSidebar from '@/views/Settings/SettingsSidebar.vue';

@Component({
  components: {
    SettingsSidebar,
  },
})
export default class SettingsPage extends Vue {
  
}
</script>

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
    font-size: 14px;
  }
}
</style>
