<template>
  <div class="settings-sidebar">
    <router-link to="/" class="back">
      <font-awesome-icon icon="chevron-left" class="mr-2" />
      Back to app
    </router-link>

    <main>
      <nav>
        <div class="heading">Settings</div>
        <router-link :to="{ name: 'instance-settings' }" class="item">
          <font-awesome-icon icon="gamepad" />
          <span>Instances</span>
        </router-link>
        <router-link :to="{ name: 'download-settings' }" class="item">
          <font-awesome-icon icon="cloud-download-alt" />
          <span>Downloads</span>
        </router-link>
        <router-link :to="{ name: 'app-settings' }" class="item">
          <font-awesome-icon icon="rocket" />
          <span>App</span>
        </router-link>
      </nav>
      <nav>
        <div class="heading">Info</div>
        <router-link :to="{ name: 'app-info' }" class="item app-info-item">
          <font-awesome-icon icon="info" />
          <span>App info</span>
        </router-link>
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
  background-color: var(--color-background);
  width: 280px;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;

  .back {
    opacity: 0.8;
    transition: opacity 0.25s ease-in-out;

    &:hover {
      opacity: 1;
    }
  }

  main {
    flex: 1;
  }

  nav {
    margin-top: 2rem;

    .heading {
      font-weight: bold;
      opacity: 0.5;
      margin-bottom: 1rem;
      font-size: 0.875rem;
    }

    .item {
      padding: 0.6rem 0.8rem;
      margin-bottom: 0.5rem;
      border-radius: 5px;
      display: flex;
      align-items: center;
      cursor: pointer;
      transition: background-color 0.25s ease-in-out;

      &:hover {
        background: var(--color-navbar);
      }

      &.router-link-exact-active,
      &.app-info-item.router-link-active {
        background: var(--color-primary-button);
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
