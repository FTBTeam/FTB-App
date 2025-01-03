<template>
  <div class="settings-sidebar">
    <router-link to="/" class="back">
      <font-awesome-icon icon="arrow-left" class="mr-2" />
      Back to app
    </router-link>

    <main>
      <nav>
        <div class="heading">Settings</div>
        <router-link :to="{ name: RouterNames.SETTINGS_APP }" class="item">
          <font-awesome-icon icon="rocket" />
          <span>App</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_INSTANCE }" class="item">
          <font-awesome-icon icon="gamepad" />
          <span>Instance Defaults</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_DOWNLOAD }" class="item">
          <font-awesome-icon icon="cloud-download-alt" />
          <span>Downloads</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_PROXY }" class="item">
          <font-awesome-icon icon="shuffle" />
          <span>Proxy</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_INTEGRATION }" class="item">
          <font-awesome-icon icon="link" />
          <span>Integrations</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_PRIVACY }" class="item">
          <font-awesome-icon icon="user-secret" />
          <span>Privacy</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_ADVANCED }" class="item">
          <font-awesome-icon icon="cogs" />
          <span>Advanced</span>
        </router-link>
      </nav>
      <nav v-if="getMtAccount">
        <div class="heading">Integrations</div>
        <router-link :to="{ name: RouterNames.SETTINGS_MT_INTEGRATION }" class="item app-info-item">
          <img src="@/assets/images/mt-logo.webp" alt="" />
          <span>MineTogether</span>
        </router-link>
      </nav>
      <nav>
        <div class="heading">Info</div>

        <router-link :to="{ name: RouterNames.SETTINGS_CHANGELOGS }" class="item app-info-item">
          <font-awesome-icon icon="info" />
          <span>Changelogs</span>
        </router-link>
      </nav>
    </main>

    <div class="meta">
      <span>App version</span>
      <popover position="bottom">
        <div class="value copyable pr-3">{{ configData.version }}</div>
        <template #inner>
          <div class="version">
            <div class="field">
              <div class="head">Released</div>
              <div class="value copyable" :title="configData.dateCompiled | dayjsFull">{{ configData.dateCompiled | dayjsFromNow }}</div>
            </div>
            <div class="field">
              <div class="head">Branch</div>
              <div class="value copyable">{{ configData.branch }}</div>
            </div>
            <div class="field">
              <div class="head">Commit</div>
              <div class="value copyable">{{ configData.commit }}</div>
            </div>
          </div>
        </template>
      </popover>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import {Getter} from 'vuex-class';
import {RouterNames} from '@/router';
import {ns} from '@/core/state/appState';
import {MineTogetherAccount} from '@/core/@types/javaApi';

@Component
export default class SettingsSidebar extends Vue {
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;

  platform = platform;
  RouterNames = RouterNames;
  
  changesHistoryOpen: boolean = false;
  
  get configData() {
    return platform.get.config;
  }
}
</script>

<style scoped lang="scss">
.settings-sidebar {
  background-color: var(--color-background);
  width: 220px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  font-size: 14px;

  .back {
    opacity: 0.8;
    transition: opacity 0.25s ease-in-out;
    margin: 0 .5rem 1rem;

    &:hover {
      opacity: 1;
    }
  }

  main {
    flex: 1;
  }

  nav {
    margin-top: 1rem;

    .heading {
      font-weight: bold;
      opacity: 0.5;
      margin: 0 .5rem .5rem;
      font-size: 0.875rem;
    }

    .item {
      padding: 0.5rem;
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

      svg,
      img {
        margin-right: 1rem;
        width: 20px;
      }
    }
  }

  .meta {
    margin-top: 0.5rem;
    font-size: 0.875rem;
    padding: 0 .5rem;

    span {
      margin-right: 0.8rem;
      opacity: 0.8;
    }

    .value {
      font-weight: bold;
    }
  }
}

.copyable {
  user-select: text;
}
</style>
