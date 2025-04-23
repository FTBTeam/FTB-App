<script lang="ts" setup>
import {Mod} from '@/types';
import appPlatform from '@platform';
import UiButton from '@/components/ui/UiButton.vue';
import UiBadge from '@/components/ui/UiBadge.vue';
import {prettyNumber} from '@/utils/helpers/stringHelpers.ts';
import { computed } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faCheck, faDownload, faUsers } from '@fortawesome/free-solid-svg-icons';

const {
  mod,
  installedMods,
} = defineProps<{
  mod: Mod;
  installedMods: [number, number][];
}>()

const projectInstalled = computed(() => installedMods.findIndex((e) => e[0] === mod.id) !== -1);
const art = computed(() => mod.art[0]?.url ?? 'broken');
const curseLink = computed(() => mod.links.find((e) => e.type === 'curseforge'));
</script>

<template>
  <div class="mod-card-wrapper">
    <div class="mod-card">
      <div class="art">
        <img :src="art" alt="Mod artwork" />
      </div>

      <div class="main">
        <div class="content">
          <div class="about">
            <div class="name flex gap-4 items-center">{{ mod.name }} <ui-badge type="success" v-if="projectInstalled" :icon="faCheck" aria-label="This mod is already installed in your pack, installing again will cause the mod to update."> Installed</ui-badge></div>
            <div class="desc pr-10 mb-3">{{ mod.synopsis }}</div>
          </div>
          <ui-button type="success" :icon="faDownload" @click="$emit('install')">Install</ui-button>
        </div>

        <div class="numbers">
          <div class="stat" :aria-label="mod.installs.toLocaleString() + ' Downloads'" data-balloon-pos="down">
            <FontAwesomeIcon :icon="faDownload" />
            <div class="value is-value">{{ prettyNumber(mod.installs) }}</div>
          </div>

          <div class="stat" aria-label="Authors" data-balloon-pos="down">
            <FontAwesomeIcon :icon="faUsers" />
            <div class="value authors">
              {{ mod.authors.slice(0, 3).map((e) => e.name).join(', ') }}
            </div>
          </div>
          
          <div class="curse-btn" v-if="curseLink?.link" @click="() => appPlatform.utils.openUrl(curseLink?.link ?? '')">
            <img src="../../../assets/curse-logo.svg" alt="" />
          </div>
        </div>
      </div>  
    </div>
  </div>
</template>

<style lang="scss" scoped>
.curse-border {
  border-color: var(--curse-color);
  border-width: 2px;
}

.mod-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: 5px;
  background: rgba(white, 0.04);
  margin-bottom: 1rem;

  .art {
    margin-right: 1.5rem;
    min-width: 120px;

    img {
      max-width: 80px;
      border-radius: 5px;
      margin: 0 auto;
    }
  }

  .main {
    flex: 1;

    .content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .name {
        font-size: 1.1rem;
        font-weight: bold;
        margin-bottom: 0.2rem;
      }

      .about {
        user-select: text;
        flex: 1;
      }
    }

    .numbers {
      display: flex;
      align-items: center;
      gap: 1.5rem;

      .stat {
        display: flex;
        align-items: center;
        gap: .8rem;

        svg {
          opacity: 0.7;
        }

        .value {
          font-weight: bold;
        }
      }

      .curse-btn {
        width: 25px;
        margin-right: 0.5rem;
        margin-left: auto;
        align-self: flex-end;

        opacity: 0.5;
        transition: opacity 0.25s ease-in-out;
        cursor: pointer;

        &:hover {
          opacity: 1;
        }
      }
    }

    .authors {
      max-width: 200px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;

      user-select: text;
    }
  }

  .installing {
    .stats {
      margin-top: 1rem;
      display: flex;
      justify-content: space-between;

      .stat {
        .text {
          opacity: 0.7;
        }

        .value {
          font-weight: bold;
        }
      }
    }
  }
}
</style>
