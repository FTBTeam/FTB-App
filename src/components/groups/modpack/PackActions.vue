<script lang="ts" setup>
import {SugaredInstanceJson} from '@/core/types/javaApi';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
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
}
</style>
