<template>
  <div class="meta-heading" :class="{ bolder: hidePackDetails }">
    <div class="back" @click="() => $emit('back')">
      <font-awesome-icon icon="chevron-left" class="mr-2" />
      Back to {{ hidePackDetails ? 'instance' : 'library' }}
    </div>

    <div
      class="beta-tag"
      :style="{ backgroundColor: getColorForReleaseType(versionType) }"
      v-if="versionType !== 'release'"
    >
      Running {{ versionType }} version
    </div>

    <div class="meta" v-if="!instance.name || instance.name.toLowerCase() !== 'vanilla'">
      <div
        class="origin icon"
        v-if="
          (!instance.packType ? (instance.type || '').toLowerCase() !== 'curseforge' : instance.packType === 0) &&
          !instance.isImport
        "
        data-balloon-pos="left"
        aria-label="FTB Modpack"
      >
        <img src="@/assets/ftb-white-logo.svg" alt="" />
      </div>
      <div class="origin icon" v-else-if="!instance.isImport" data-balloon-pos="left" aria-label="Curseforge Modpack">
        <img src="@/assets/curse-logo.svg" alt="" />
      </div>
      <div class="modloader icon" v-if="isForgePack" data-balloon-pos="left" aria-label="Forge Modloader">
        <img src="@/assets/images/forge.svg" alt="" />
      </div>
      <div class="modloader icon" v-else data-balloon-pos="left" aria-label="Fabric Modloader">
        <img src="@/assets/images/fabric.png" alt="" />
      </div>

      <ftb-button
        v-if="instance.uuid && instance.isModified"
        :disabled="!getActiveMcProfile"
        :title="
          !getActiveMcProfile
            ? 'You need to be logged in to your Minecraft account to share packs'
            : 'Share your modpack with friends'
        "
        class="py-1 ml-6 px-4"
        color="info"
        css-class="text-center text-l"
        @click="shareConfirm = true"
      >
        <font-awesome-icon icon="share" class="" size="1x" />
      </ftb-button>
    </div>

    <share-instance-modal :open="shareConfirm" @closed="shareConfirm = false" :uuid="instance.uuid" />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { getColorForReleaseType } from '@/utils/colors';
import ShareInstanceModal from '@/components/organisms/modals/actions/ShareInstanceModal.vue';
import { Getter } from 'vuex-class';

@Component({
  components: { ShareInstanceModal },
})
export default class PackMetaHeading extends Vue {
  @Getter('getActiveProfile', { namespace: 'core' }) public getActiveMcProfile!: any;

  @Prop() hidePackDetails!: boolean;
  @Prop() isForgePack!: boolean;
  @Prop() versionType!: string;
  @Prop() instance!: any;

  getColorForReleaseType = getColorForReleaseType;

  shareConfirm = false;
}
</script>

<style lang="scss" scoped>
.meta-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(black, 0.6);
  padding: 0.8rem 1rem;
  position: relative;
  z-index: 1;
  transition: background-color 0.25s ease-in-out;

  &.bolder {
    background-color: var(--color-navbar);

    .meta {
      opacity: 0;
    }
  }

  .back {
    opacity: 0.7;
    cursor: pointer;
    transition: opacity 0.25s ease-in-out;

    &:hover {
      opacity: 1;
    }
  }

  .back,
  .meta {
    flex: 1;
  }

  .meta {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    transition: opacity 0.25s ease-in-out;

    .icon {
      cursor: default;
      margin-left: 1.5rem;
      img {
        width: 30px;
      }
    }
  }

  .beta-tag {
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    background-color: rgba(234, 32, 32, 0.89);
    font-size: 0.75rem;
    font-weight: bold;
  }
}
</style>
