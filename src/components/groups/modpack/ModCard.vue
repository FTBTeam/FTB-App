<template>
  <div class="mod-card-wrapper">
    <div class="mod-card">
      <div class="art">
        <img :src="art" alt="Mod artwork" />
      </div>

      <div class="main">
        <div class="content">
          <div class="about">
            <div class="name flex gap-4 items-center">{{ mod.name }} <ui-badge type="success" v-if="projectInstalled" icon="check" aria-label="This mod is already installed in your pack, installing again will cause the mod to update."> Installed</ui-badge></div>
            <div class="desc pr-10 mb-3">{{ mod.synopsis }}</div>
          </div>
          <ui-button type="success" icon="download" @click="$emit('install')">Install</ui-button>
        </div>

        <div class="numbers">
          <div class="stat" :aria-label="mod.installs.toLocaleString() + ' Downloads'" data-balloon-pos="down">
            <font-awesome-icon icon="download" />
            <div class="value is-value">{{ prettyNumber(mod.installs) }}</div>
          </div>

          <div class="stat" aria-label="Authors" data-balloon-pos="down">
            <font-awesome-icon icon="users" />
            <div class="value authors">
              {{ mod.authors.slice(0, 3).map((e) => e.name).join(', ') }}
            </div>
          </div>
          
          <div class="curse-btn" v-if="curseLink.link" @click="() => platform.get.utils.openUrl(curseLink.link)">
            <img src="../../../assets/curse-logo.svg" alt="" />
          </div>
        </div>
      </div>  
    </div>
  </div>
</template>

<script lang="ts">
import {Mod, ModVersion} from '@/types';
import {Component, Prop, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import UiButton from '@/components/ui/UiButton.vue';
import Selection2 from '@/components/ui/Selection2.vue';
import {InstanceJson} from '@/core/@types/javaApi';
import UiBadge from '@/components/ui/UiBadge.vue';
import {prettyNumber} from '../../../utils/helpers/stringHelpers';

@Component({
  methods: {prettyNumber},
  components: {
    UiBadge,
    UiButton,
    Selection2,
  },
})
export default class ModCard extends Vue {
  @Prop() mod!: Mod;
  @Prop() instance!: InstanceJson;
  @Prop() target!: string;

  @Prop() installedMods!: [number, number][];

  platform = platform;

  static fileName(modName: string, fileName: string, trim = -1, trimReverse = false) {
    const cleanedModName = modName.split("-")[0].replaceAll(" ", "").replace(/[^a-z0-9]/gi, '_').toLowerCase();
    
    // Try and remove the modname from the file name
    let replacedName = fileName.replace(new RegExp(cleanedModName, "gi"), "").replace(".jar", "").toLowerCase();
    
    if (replacedName.startsWith("-") || replacedName.startsWith("_")) {
      replacedName = replacedName.substring(1);
    }
    
    const variations = ["$1", "$1_", "$1-", "-$1", "_$1", "_$1_", "-$1-"].reverse();
    const lookups  = ["forge", "fabric", "quilt", "neoforge", "mc", "minecraft", "release", "alpha", "beta"];
    
    // Soft search to save on performance
    // Look over the lookups list and see if the name contains any of them
    if (lookups.findIndex((e) => replacedName.includes(e)) !== -1) {
      for (const variation of variations) {
        for (const lookup of lookups) {
          const regex = new RegExp(`(${variation.replace("$1", lookup)})`, "gi");
          replacedName = replacedName.replace(regex, "");
        }
      }
    }
    
    if (trim === -1 || replacedName.length <= trim) {
      return replacedName;
    }
    
    return !trimReverse ? ("..." + replacedName.substring(0, trim)) : (replacedName.substring(replacedName.length - trim) + "...")
  }
  
  static fileNameFromMod(mod: Mod, file: ModVersion) {
    return ModCard.fileName(mod.name, file.name);
  }
  
  _fileName(fileName: string) {
    return ModCard.fileName(this.mod.name, fileName, 20);
  }
  
  get projectInstalled() {
    return this.installedMods.findIndex((e) => e[0] === this.mod.id) !== -1;
  }
  
  get projectFileInstalled() {
    return this.installedMods.findIndex((e) => e[1] === this.versions[this.versions.length - 1].id) !== -1;
  }
  
  get art() {
    return this.mod.art[0]?.url ?? 'broken';
  }

  get latest() {
    return this.versions[this.versions.length - 1] ? this.versions[this.versions.length - 1].name : 'Unknown';
  }

  get curseLink() {
    return this.mod.links.find((e) => e.type === 'curseforge');
  }
  
  get versions() {
    return this.mod.versions
        .filter(
          (e) =>
            e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === this.target) !== -1,
        )
        .sort((a, b) => b.id - a.id) ?? [];
  }
}
</script>

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
