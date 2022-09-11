<template>
  <div class="pack-actions-holder">
    <div class="pack-actions" tabindex="0">
      <div class="icon">
        <font-awesome-icon icon="ellipsis-vertical" />
      </div>

      <ul class="actions">
        <li class="title">Tools</li>
        <li @click="$emit('openSettings')">
          <span><font-awesome-icon icon="cog" /></span>Settings
        </li>
        <li tabindex="1">
          <span><font-awesome-icon icon="folder-open" /></span>Open...
          <span class="submenu">
            <font-awesome-icon icon="chevron-right" />
          </span>
          <ul>
            <li @click="openInstanceFolder('')">Instance folder</li>
            <li v-if="folderExists('config')" @click="openInstanceFolder('config')">Config folder</li>
            <li v-if="folderExists('mods')" @click="openInstanceFolder('mods')">Mods folder</li>
            <li v-if="folderExists('saves')" @click="openInstanceFolder('saves')">Worlds folder</li>
            <li v-if="folderExists('backups')" @click="openInstanceFolder('backups')">Backups folder</li>
            <li v-if="folderExists('resourcepacks')" @click="openInstanceFolder('resourcepacks')">
              Resource packs folder
            </li>
            <li v-if="folderExists('logs')" @click="openInstanceFolder('logs')">Logs folder</li>
            <li v-if="folderExists('crashlogs')" @click="openInstanceFolder('crashlogs')">Crash logs folder</li>
            <li v-if="folderExists('kubejs')" @click="openInstanceFolder('kubejs')">KubeJS folder</li>
          </ul>
        </li>
        <li @click="duplicateConfirm = true">
          <span><font-awesome-icon icon="copy" /></span>Duplicate instance
        </li>
        <li @click="shareConfirm = true">
          <span><font-awesome-icon icon="share" /></span>Share instance
        </li>
        <li class="title">Danger</li>
        <li @click="deleteConfirm = true">
          <span><font-awesome-icon icon="trash" /></span>Delete instance
        </li>
      </ul>
    </div>

    <share-instance-modal :open="shareConfirm" @closed="shareConfirm = false" :uuid="instance.uuid" />

    <modal
      :open="duplicateConfirm"
      :externalContents="true"
      @closed="duplicateConfirm = false"
      title="Duplicate Instance"
      subTitle="Are you sure?!"
    >
      <duplicate-instance-modal
        @finished="duplicateConfirm = false"
        :uuid="instance.uuid"
        :instanceName="instance.name"
      />
    </modal>

    <modal
      :open="deleteConfirm"
      :externalContents="true"
      @closed="deleteConfirm = false"
      title="Delete Instance"
      subTitle="Are you sure?!"
    >
      <delete-instance-modal :uuid="instance.uuid" :instanceName="instance.name" />
    </modal>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import { wsTimeoutWrapperTyped } from '@/utils';
import { Prop } from 'vue-property-decorator';
import { Instance } from '@/modules/modpacks/types';
import ShareInstanceModal from '@/components/organisms/modals/actions/ShareInstanceModal.vue';
import DeleteInstanceModal from '@/components/organisms/modals/actions/DeleteInstanceModal.vue';
import DuplicateInstanceModal from '@/components/organisms/modals/actions/DuplicateInstanceModal.vue';

@Component({
  components: {
    ShareInstanceModal,
    DeleteInstanceModal,
    DuplicateInstanceModal,
  },
})
export default class PackActions extends Vue {
  @Prop() instance!: Instance;

  instanceFolders: string[] = [];
  shareConfirm = false;
  duplicateConfirm = false;
  deleteConfirm = false;

  mounted() {
    wsTimeoutWrapperTyped<any, { folders: string[] }>({ type: 'getInstanceFolders', uuid: this.instance.uuid })
      .then((e) => (this.instanceFolders = e.folders))
      .catch(console.log);
  }

  async openInstanceFolder(folder: string) {
    await wsTimeoutWrapperTyped<any, null>({
      type: 'instanceBrowse',
      uuid: this.instance.uuid,
      folder,
    });
  }

  folderExists(path: string) {
    if (path === '' || !this.instanceFolders.length) {
      return false;
    }

    return this.instanceFolders.findIndex((e) => e === path) !== -1;
  }
}
</script>

<style lang="scss">
.pack-actions-holder {
  display: flex;
}

.pack-actions {
  display: flex;
  align-items: center;
  margin-left: -5px;
  border-radius: 5px;
  background-color: rgba(black, 0.5);
  cursor: pointer;

  .icon {
    padding: 0 1.2rem 0 1.7rem;
  }

  .actions,
  .actions li ul {
    font-size: 0.875rem;
    position: absolute;
    white-space: nowrap;
    top: 110%;
    left: 20px;
    background-color: black;
    z-index: 500;
    padding: 1rem 0.8rem;
    border-radius: 5px;
    box-shadow: 0 4px 15px rgba(black, 0.2);
    border: 1px solid rgba(white, 0.1);
    opacity: 0;
    visibility: hidden;

    transition: visibility 0.25s ease-in-out, left 0.25s ease-in-out, opacity 0.25s ease-in-out;

    &::after {
      content: '';
      position: absolute;
      top: -8px;
      left: 15px;
      border-top: 8px solid black;
      border-left: 8px solid black;
      border-radius: 2px;
      width: 15px;
      height: 15px;
      transform: rotateZ(45deg);
      box-shadow: 0 0 15px rgba(black, 0.2);
    }

    li {
      line-height: 2.2em;
      display: flex;
      padding: 0 0.8rem;
      border-radius: 5px;
      transition: background-color 0.15s ease-in-out;

      &.title {
        background-color: transparent !important;
        cursor: default;
      }

      &:hover,
      &:focus-within {
        background-color: var(--color-info-button);
      }

      span {
        width: 35px;
      }

      .submenu {
        width: unset;
        margin-left: auto;
        font-size: 0.875em;
      }

      &:not(:last-child) {
        margin-bottom: 0.5rem;
      }
    }

    .title {
      font-weight: bold;
      color: rgba(white, 0.5);
    }
  }

  .actions li ul {
    left: 110%;
    top: 0;

    &::after {
      transform: rotateZ(-45deg);
      top: 1rem;
      left: -8px;
    }
  }

  &:focus-within {
    .actions {
      visibility: visible;
      opacity: 1;
      left: 0;
    }
  }

  .actions li:focus-within ul {
    visibility: visible;
    opacity: 1;
    left: 102%;
  }
}
</style>
