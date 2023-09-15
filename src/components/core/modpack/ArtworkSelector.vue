<template>
  <div class="artwork-selector flex items-center gap-6">
    <p v-if="isDraggingOver"> I'm ready daddy </p>
    <img width="120" :src="getArtwork" alt="" />
    <div class="actions flex flex-col items-start">
      <label>
        <input hidden="hidden" type="file" :accept="allowedFileTypes.join(',')"  @change="processFileList($event.target.files)" />
        <ftb-button color="info" class="px-6 py-2">
          <font-awesome-icon icon="upload" class="mr-4" />
          <span>Upload your own</span>
        </ftb-button>
      </label>
      <ftb-button color="danger" class="px-4 py-1 mt-4" v-if="value" @click="$emit('input', null)">
        <font-awesome-icon icon="upload" class="mr-2" />
        <span>Remove</span>
      </ftb-button>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Prop} from 'vue-property-decorator';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {InstanceJson} from '@/core/@types/javaApi';
import {ModPack} from '@/modules/modpacks/types';

@Component({
  methods: {resolveArtwork}
})
export default class ArtworkSelector extends Vue {
  @Prop() value!: File | null;
  @Prop() pack?: InstanceJson | ModPack;
  
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
    }
  }
}
</script>

<style lang="scss" scoped>
.artwork-selector {
  img {
    border-radius: 8px;
    border: 1px solid rgba(white, .1);
  }
}
</style>