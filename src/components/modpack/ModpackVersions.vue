<template>
  <div class="pack-versions py-4">
    <div class="aside">
      <p>Versions</p>
      <div class="items">
        <div class="item" :class="{ active: index === 0 }" v-for="(version, index) in versions" :key="index">
          <div class="main">
            <div class="version">{{ version.name }}</div>
            <div class="updated">{{ version.updated | momentFromNow }}</div>
          </div>
          <div
            class="type"
            :style="{
              backgroundColor:
                version.type.toLowerCase() === 'release'
                  ? '#27AE60'
                  : version.type.toLowerCase() === 'beta'
                  ? '#00a8FF'
                  : 'black',
            }"
          >
            {{ version.type }}
          </div>
        </div>
      </div>
    </div>
    <div class="main">
      <p>Changelog</p>
      <div class="body-contents">
        No changelog data
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Versions } from '@/modules/modpacks/types';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class ModpackVersions extends Vue {
  @Prop() versions!: Versions[];
  @Prop() current!: number;
}
</script>

<style scoped lang="scss">
.pack-versions {
  display: flex;

  .aside,
  .main {
    > p {
      margin-bottom: 1rem;
      font-size: 1.25rem;
    }
  }

  .aside {
    margin-right: 2rem;

    .items {
      width: 250px;

      .item {
        display: flex;
        align-items: center;
        justify-content: space-between;

        margin-bottom: 1rem;
        padding: 0.8rem 1rem;
        border-radius: 5px;

        &.active {
          background-color: var(--color-background);
        }

        .main {
          .version {
            font-weight: bold;
            font-family: Arial, Helvetica, sans-serif;
          }
        }

        .type {
          margin-bottom: 0.2rem;
          display: inline-block;
          padding: 0.1rem 0.3rem;
          border-radius: 3px;
          background-color: pink;
          font-size: 0.875rem;
          margin-left: auto;
          text-transform: capitalize;
        }

        .updated {
          font-size: 0.875rem;
          opacity: 0.8;
        }
      }
    }
  }

  .main {
    flex: 1;
  }
}
</style>
