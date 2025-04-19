<script lang="ts" setup>
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {InstanceJson} from '@/core/types/javaApi';
import { UiButton } from '@/components/ui';
import { useAttachDomEvent } from '@/composables';
import { computed, ref } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { ModPack } from '@/core/types/appTypes.ts';
import { faTrash, faUpload } from '@fortawesome/free-solid-svg-icons';

const allowedFileTypes = [
  "image/png",
  "image/jpeg",
  "image/gif"
]

const value = defineModel<File | null>()

const {
  allowRemove = true,
  pack = null
} = defineProps<{
  allowRemove?: boolean;
  pack?: InstanceJson | ModPack;
}>()

const isDraggingOver = ref(false)

useAttachDomEvent<DragEvent>('dragover', onDragStart)
useAttachDomEvent<DragEvent>('dragleave', onDragEnd)
useAttachDomEvent<DragEvent>('drop', drop)

function onDragStart(event: DragEvent) {
  event.preventDefault();
  event.stopPropagation();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy';
  }
  isDraggingOver.value = true;
}

function onDragEnd(event: DragEvent) {
  event.preventDefault();
  event.stopPropagation();
  if (event.x === 0 && event.y === 0) {
    isDraggingOver.value = false;
  }
}

function drop(event: DragEvent) {
  event.preventDefault();
  event.stopPropagation();
  
  isDraggingOver.value = false;
  const files = event.dataTransfer?.files;
  if (!files?.length) {
    return;
  }
  
  const fileList: File[] = [];
  for (const item of files) {
    if (item instanceof File) {
      fileList.push(item);
    }
  }
  
  processFileList(fileList);
}

function processFileList(files: File[]) {
  console.log(files)
  let firstValidFile: File | null = null;
  for (const file of files) {
    if (allowedFileTypes.includes(file.type)) {
      firstValidFile = file;
      break;
    }
  }
  
  console.log(firstValidFile, firstValidFile?.path)
  if (firstValidFile) {
    value.value = firstValidFile;
  }
}

const getArtwork = computed(() => {
  if (value.value) {
    return "file://" + value.value.arrayBuffer;
  }

  return resolveArtwork(pack ?? null);
})

const hovering = isDraggingOver.value;
</script>

<template>
  <div class="artwork-selector flex items-center gap-6" :class="{'is-hovering': hovering}">
    <div class="drop-indicator" v-if="hovering">
      <div class="text flex flex-col gap-4">
        <FontAwesomeIcon :icon="faUpload" size="lg" />
        <b>Drop artwork here</b>
      </div>
    </div>
    <img width="120" :src="getArtwork" alt="" />
    <div class="actions flex flex-col items-start">
      <label>
        <input hidden="hidden" type="file" :accept="allowedFileTypes.join(',')"  @change="processFileList(($event.target as any)?.files)" />
        <UiButton type="info" :icon="faUpload">
          Update artwork
        </UiButton>
      </label>
      <UiButton :icon="faTrash" size="small" class="mt-3" type="warning" v-if="value && allowRemove" @click="value = null">
        Remove
      </UiButton>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.artwork-selector {
  position: relative;
  img {
    border-radius: 8px;
    border: 1px solid rgba(white, .1);
  }
  
  .actions, > img {
    transition: .25s ease-in-out opacity;
  }
  
  &.is-hovering {
    img, .actions {
      opacity: 0;
    }
  }
}

.drop-indicator {
  position: absolute;
  inset: 0;
  background: rgba(white, .1);
  border-radius: 8px;
  border: 1px dashed rgba(white, .2);
  z-index: 1;
  pointer-events: none;
  animation: pulse 1.2s infinite;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>