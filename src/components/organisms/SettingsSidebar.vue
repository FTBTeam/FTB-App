<template>
  <div class="settings-sidebar">
    <router-link to="/" class="back">
      <font-awesome-icon icon="arrow-left" class="mr-2" />
      Back to app
    </router-link>

    <main>
      <nav>
        <div class="heading">Settings</div>
        <router-link :to="{ name: RouterNames.SETTINGS_INSTANCE }" class="item">
          <font-awesome-icon icon="gamepad" />
          <span>Instances</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_DOWNLOAD }" class="item">
          <font-awesome-icon icon="cloud-download-alt" />
          <span>Downloads</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_APP }" class="item">
          <font-awesome-icon icon="rocket" />
          <span>App</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_INTEGRATION }" class="item">
          <font-awesome-icon icon="link" />
          <span>Integrations</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_PROXY }" class="item">
          <font-awesome-icon icon="shuffle" />
          <span>Proxy</span>
        </router-link>
      </nav>
      <nav v-if="auth.token !== null">
        <div class="heading">Integrations</div>
        <router-link :to="{ name: RouterNames.SETTINGS_MT_INTEGRATION }" class="item app-info-item">
          <img src="@/assets/images/mt-logo.webp" alt="" />
          <span>MineTogether</span>
        </router-link>
      </nav>
      <nav>
        <div class="heading">Info</div>

        <div @click="changesHistoryOpen = true" class="item app-info-item">
          <font-awesome-icon icon="info" />
          <span>Changelogs</span>
        </div>
      </nav>
    </main>

    <div class="meta">
      <span>App version</span>
      <popover position="bottom">
        <div class="value copyable pr-3">{{ version }}</div>
        <template #inner>
          <div class="version">
            <div class="field mb-4">
              <div class="head">UI Version</div>
              <div class="value copyable">{{ uiVersion }}</div>
            </div>
            <div class="field">
              <div class="head">Subprocess Version</div>
              <div class="value copyable">{{ appVersion }}</div>
            </div>
          </div>
        </template>
      </popover>
    </div>

    <!--    <modal-->
    <!--      :open="changesHistoryOpen"-->
    <!--      title="Changelogs"-->
    <!--      subTitle="Checkout the changes we've been making!"-->
    <!--      size="medium"-->
    <!--      @closed="changesHistoryOpen = false"-->
    <!--    >-->
    <!--      <changelog-history v-if="changesHistoryOpen" />-->
    <!--    </modal>-->
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import {State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import ChangelogHistory from '@/components/templates/changelogs/ChangelogHistory.vue';
import {RouterNames} from '@/router';

@Component({
  components: { ChangelogHistory },
})
export default class SettingsSidebar extends Vue {
  @State('auth') private auth!: AuthState;

  platform = platform;
  RouterNames = RouterNames;

  version = platform.get.config.publicVersion;
  uiVersion = platform.get.config.webVersion;
  appVersion = platform.get.config.appVersion;
  changesHistoryOpen: boolean = false;
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
    display: flex;
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
