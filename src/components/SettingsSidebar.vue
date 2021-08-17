<template>
  <div class="settings-sidebar">
    <div class="back">
      <font-awesome-icon icon="chevron-left" class="mr-2" />
      Back to app
    </div>

    <main>
      <nav>
        <div class="heading">Settings</div>
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
      </nav>
      <nav>
        <div class="heading">Info</div>
        <div class="item">
          <font-awesome-icon icon="info" />
          <span>App info</span>
        </div>
      </nav>
    </main>

    <div class="meta">
      <div class="field">
        <div class="head">UI Version</div>
        <div class="value">{{ uiVersion }}</div>
      </div>
      <div class="field">
        <div class="head">Subprocess Version</div>
        <div class="value">{{ appVersion }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class SettingsSidebar extends Vue {
  platform = platform;

  uiVersion: string = platform.get.config.webVersion;
  appVersion: string = platform.get.config.appVersion;
}
</script>

<style scoped lang="scss">
.settings-sidebar {
  background-color: var(--color-navbar);
  width: 300px;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;

  main {
    flex: 1;
  }

  nav {
    margin-top: 2rem;

    .heading {
      font-weight: bold;
      opacity: 0.5;
      margin-bottom: 1rem;
    }

    .item {
      padding: 0.7rem 1rem;
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

  .meta {
    .field {
      margin-top: 0.5rem;
      .head {
        font-weight: bold;
        opacity: 0.5;
        font-size: 0.875rem;
        margin-bottom: 0.2rem;
      }

      .value {
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}
</style>
