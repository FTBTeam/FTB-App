<script lang="ts" setup>
import DuplicateInstanceModal from '@/components/modals/actions/DuplicateInstanceModal.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {createLogger} from '@/core/logger';
import { onMounted, ref } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { Modal } from '@/components/ui';
import {
  faEllipsisVertical,
} from '@fortawesome/free-solid-svg-icons';
import {AppContextController} from "@/core/context/contextController.ts";
import {ContextMenus} from "@/core/context/contextMenus.ts";

const {
  instance,
} = defineProps<{
  instance: SugaredInstanceJson;
}>()

const logger = createLogger("PackActions.vue")
const instanceFolders = ref<string[]>([]);
const duplicateConfirm = ref(false);

onMounted(() => {
  sendMessage('getInstanceFolders', { uuid: instance.uuid })
    .then((e) => (instanceFolders.value = e.folders))
    .catch(e => logger.error("Failed to get instance folders", e));
})

function openMenu(e: MouseEvent) {
  e.stopPropagation()
  e.preventDefault();

  AppContextController.openMenu(ContextMenus.INSTANCE_MENU, e, () => ({
    instance: instance
  }))
}
</script>

<template>
  <div class="pack-actions-holder">
    <div class="pack-actions">
      <div class="icon" @click="(e) => openMenu(e)">
        <FontAwesomeIcon :icon="faEllipsisVertical" />
      </div>
    </div>
    
<!-- TODO: Fix me, this doesn't really work in the new context menu version -->
    <Modal
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
        :category="instance.category"
      />
    </Modal>
  </div>
</template>

<style scoped lang="scss">
.pack-actions-holder {
  display: flex;
}

.pack-actions {
  display: flex;
  position: relative;
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
    position: absolute;
    white-space: nowrap;
    top: -7rem;
    left: 140%;
    background-color: var(--color-navbar);
    z-index: 500;
    padding: .7rem 0.5rem;
    border-radius: 5px;
    box-shadow: 0 4px 15px rgba(black, 0.2);
    border: 1px solid rgba(white, 0.1);
    opacity: 0;
    visibility: hidden;

    transition: visibility 0.25s ease-in-out, left 0.25s ease-in-out, opacity 0.25s ease-in-out;

    &::after {
      content: '';
      position: absolute;
      top: calc(7rem + 15px);
      left: -8px;
      border-top: 8px solid var(--color-navbar);
      border-left: 8px solid var(--color-navbar);
      border-radius: 4px;
      width: 15px;
      height: 15px;
      transform: rotateZ(-45deg);
    }

    li {
      position: relative;
      line-height: 2.4em;
      display: flex;
      padding: 0 0.8rem;
      border-radius: 5px;
      transition: background-color 0.15s ease-in-out;

      &.title {
        background-color: transparent !important;
        cursor: default;
        line-height: 1.2em;
      }

      &:hover {
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
    left: 100%;
    top: calc(1rem - 22px);

    &::before {
      content: '';
      position: absolute;
      width: 18px;
      top: 0;
      left: -18px;
      height: 100%;
    }

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
      left: 125%;
    }
  }

  .actions li:hover ul {
    visibility: visible;
    opacity: 1;
    left: 115%;
  }
}
</style>
