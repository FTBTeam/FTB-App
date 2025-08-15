<script lang="ts" setup>
import appPlatform from '@platform';
import {RouterNames} from '@/router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { standardDateTime, timeFromNow } from '@/utils/helpers/dateHelpers.ts';
import { Popover } from '@/components/ui';
import {
  faArrowLeft, faCogs,
  faDownload,
  faGamepad, faInfo,
  faRocket,
  faShuffle,
  faUserSecret,
} from '@fortawesome/free-solid-svg-icons';

const configData = appPlatform.config;
</script>


<template>
  <div class="settings-sidebar">
    <router-link to="/" class="back">
      <FontAwesomeIcon :icon="faArrowLeft" class="mr-2" />
      Back to app
    </router-link>

    <main>
      <nav>
        <div class="heading">Settings</div>
        <router-link :to="{ name: RouterNames.SETTINGS_APP }" class="item">
          <FontAwesomeIcon :icon="faRocket" />
          <span>App</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_INSTANCE }" class="item">
          <FontAwesomeIcon :icon="faGamepad" />
          <span>Instance Defaults</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_DOWNLOAD }" class="item">
          <FontAwesomeIcon :icon="faDownload" />
          <span>Downloads</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_PROXY }" class="item">
          <FontAwesomeIcon :icon="faShuffle" />
          <span>Proxy</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_PRIVACY }" class="item">
          <FontAwesomeIcon :icon="faUserSecret" />
          <span>Privacy</span>
        </router-link>
        <router-link :to="{ name: RouterNames.SETTINGS_ADVANCED }" class="item">
          <FontAwesomeIcon :icon="faCogs" />
          <span>Advanced</span>
        </router-link>
      </nav>
      <nav>
        <div class="heading">Info</div>

        <router-link :to="{ name: RouterNames.SETTINGS_CHANGELOGS }" class="item app-info-item">
          <FontAwesomeIcon :icon="faInfo" />
          <span>Changelogs</span>
        </router-link>
      </nav>
    </main>

    <div class="meta">
      <span>App version</span>
      <Popover position="bottom">
        <div class="value copyable pr-3">{{ configData.version }}</div>
        <template #inner>
          <div class="version">
            <div class="field">
              <div class="head">Released</div>
              <div class="value copyable" :title="standardDateTime(configData.dateCompiled)">{{ timeFromNow(configData.dateCompiled) }}</div>
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
      </Popover>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import 'tailwindcss/theme' theme(reference);

.settings-sidebar {
  background-color: var(--color-background);
  width: 220px;
  padding: 1rem;
  display: flex;
  flex-direction: column;

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
        background: var(--color-green-600);
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
