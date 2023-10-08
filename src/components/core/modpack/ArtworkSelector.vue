<template>
  <div class="artwork-selector flex items-center gap-6" :class="{'is-hovering': hovering}">
    <div class="drop-indicator" v-if="hovering">
      <div class="text flex flex-col gap-4">
        <font-awesome-icon icon="upload" size="lg" />
        <b>Drop artwork here</b>
      </div>
    </div>
    <img width="120" :src="getArtwork" alt="" />
    <div class="actions flex flex-col items-start">
      <label>
        <input hidden="hidden" type="file" :accept="allowedFileTypes.join(',')"  @change="processFileList($event.target.files)" />
        <ui-button type="info" icon="upload">
          Upload image
        </ui-button>
      </label>
      <ui-button icon="trash" size="small" class="mt-3" type="warning" v-if="value && allowRemove" @click="() => {
        $emit('input', null);
        $emit('change', null);
      }">
        Remove
      </ui-button>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Prop} from 'vue-property-decorator';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {InstanceJson} from '@/core/@types/javaApi';
import {ModPack} from '@/modules/modpacks/types';
import UiButton from '@/components/core/ui/UiButton.vue';

// TODO: Make this look a lot nicer
@Component({
  components: {UiButton},
  methods: {resolveArtwork}
})
export default class ArtworkSelector extends Vue {
  @Prop() value!: File | null;
  @Prop() pack?: InstanceJson | ModPack;

  @Prop({default: true}) allowRemove!: boolean;
  
  isDraggingOver = false;
  private allowedFileTypes = [
    "image/png",
    "image/jpeg",
    "image/gif"
  ]
  
  mounted() {
    // Add drag and drop events
    document.addEventListener("dragover", this.onDragStart);
    document.addEventListener("dragleave", this.onDragEnd);
    document.addEventListener("drop", this.drop);
  }
  
  destroyed() {
    // Remove drag and drop events
    document.removeEventListener("dragover", this.onDragStart);
    document.removeEventListener("dragleave", this.onDragEnd);
    document.removeEventListener("drop", this.drop);
  }
  
  get getArtwork() {
    if (this.value && "path" in this.value) {
      return "file://" + this.value.path;
    }
    
    return resolveArtwork(this.pack ?? null);
  }

  private onDragStart(event: any) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = 'copy';
    this.isDraggingOver = true;
  }
  
  private onDragEnd(event: any) {
    event.preventDefault();
    event.stopPropagation();
    if (event.x === 0 && event.y === 0) {
      this.isDraggingOver = false;
    }
  }
  
  private drop(event: any) {
    event.preventDefault();
    event.stopPropagation();
    
    this.isDraggingOver = false;
    const files = event.dataTransfer.files;
    this.processFileList(files);
  }
  
  processFileList(files: File[]) {
    let firstValidFile: File | null = null;
    for (const file of files) {
      if (this.allowedFileTypes.includes(file.type)) {
        firstValidFile = file;
        break;
      }
    }

    if (firstValidFile) {
      this.$emit('input', firstValidFile);
      this.$emit('change', firstValidFile);
    }
  }
  
  get hovering() {
    return this.isDraggingOver
  }
}
</script>

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