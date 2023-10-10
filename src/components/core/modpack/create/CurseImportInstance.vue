<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Import from CurseForge" sub-title="Import instances from a CurseForge .zip export file">
    <modal-body>
      <div
        class="drop-area"
        :class="{'has-file': activeFile}"
        @click="$refs.fileInputRef.click()"
        @dragenter.prevent
        @dragleave.prevent
        @dragover.prevent
        @drop.prevent="fileAttach($event)"
      >
        <font-awesome-icon icon="upload" class="mr-2" size="2x" />
        <p>Drag & Drop a file or select a file</p>
        <input type="file" @change="fileAttach($event)" accept="application/zip" hidden ref="fileInputRef" />
      </div>

      <transition name="transition-fade" duration="250">
        <p v-if="activeFile" class="font-bold mt-4 text-base mb-2">Selected file</p>
      </transition>
      
      <transition name="transition-fade" duration="250">
        <div class="file flex items-center p-4 pl-6" v-if="activeFile">
          <font-awesome-icon icon="file-zipper" size="2x" class="mr-6" />
          <div class="text flex-1">
            <div class="name font-bold">{{ activeFile.name }}</div>
            <div class="size opacity-75">
              {{ prettyByteFormat(activeFile.size) }}
            </div>
          </div>
          <div class="delete" @click="activeFile = null">
            <font-awesome-icon icon="trash" />
          </div>
        </div>
      </transition>

      <category-selector class="mt-4" label="Import to category" v-model="category" />
    </modal-body>
    
    <modal-footer>
      <div class="flex justify-end">
        <ui-button :wider="true" :disabled="!activeFile" type="success" icon="upload" @click="installZip">
          Install
        </ui-button>
      </div>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import Modal from '@/components/atoms/modal/Modal.vue';
import {prettyByteFormat} from '@/utils';
import UiButton from '@/components/core/ui/UiButton.vue';
import {alertController} from '@/core/controllers/alertController';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {RouterNames} from '@/router';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';

@Component({
  components: {CategorySelector, UiButton, Modal, ModalBody},
  methods: {prettyByteFormat}
})
export default class CurseImportInstance extends Vue {
  @Prop() open!: boolean;
  @Emit() close() {}
  
  activeFile: any = null;
  category = "Default"
  
  async fileAttach(event: any) {
    const file = event.dataTransfer?.files[0] ?? event.target?.files[0] ?? null;
    if (file == null || !file.name.endsWith('.zip')) {
      alertController.warning('Please select a valid .zip file.')
      return;
    }

    const res = await sendMessage('checkCurseZip', {
      path: file.path ?? 'invalid-path-name-to-break-the-java-size-by-default'
    });
    
    if (!res.success) {
      alertController.error(res.message ?? "We're unable to detect a CurseForge pack in this zip file.")
      return;
    }
    
    this.activeFile = {
      name: file.name,
      size: file.size,
      path: file.path,
    };
  }

  async installZip() {
    if (!this.activeFile) {
      console.log('no file selected')
      return;
    }

    await instanceInstallController.requestImport(this.activeFile.path, this.category)
    console.log(this.activeFile.path)
    this.activeFile = null;
    
    await gobbleError(() => {
      this.$router.push({
        name: RouterNames.ROOT_LIBRARY
      })
    })
    
    this.close();
  }
}
</script>

<style lang="scss" scoped>
.drop-area {
  margin-top: 1rem;
  padding: 5rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed rgba(white, 0.2);
  border-radius: 5px;
  
  hr {
    margin: 1rem 0;
  }

  > svg {
    margin-bottom: 1rem;
  }
}

.file {
  background-color: rgba(white, 0.1);
  border-radius: 5px;

  .text {
    .name {
      word-wrap: break-word;
    }
  }
  
  .delete {
    padding: .5rem;
    cursor: pointer;
  }
}
</style>