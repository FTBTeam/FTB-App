<template>
  <div class="create-instance-container">
    <popover :text="open ? '' : 'Create an instance'">
      <div class="action-icon" @click="open = true">
        <font-awesome-icon icon="plus" />
      </div>
    </popover>
    <div class="dropdown" :class="{active: open}">
      <div class="options">
        <div class="option create" @click="() => {
           open = false;
           createOpen = true;
        }">
          <div class="icon"><font-awesome-icon icon="circle-plus" /></div>
          <p>Create instance</p>
          <small>Build your own vanilla or modded experience</small>
        </div>
        <div class="separator"></div>
        <div class="option import" @click="() => {
          open = false;
          importOpen = true;
        }">
          <div class="icon"><font-awesome-icon icon="file-import" /></div>
          <p>Import</p>
          <small>Import instances from other launchers</small>
        </div>
        <div class="option share" @click="() => {
            open = false;
            shareOpen = true;
        }">
          <div class="icon"><font-awesome-icon icon="code" /></div>
          <p>Share codes</p>
          <small>Use a share code to import a friends instance</small>
        </div>
        <div class="option curse" @click="() => {
          open = false;
          curseOpen = true;
        }">
          <div class="icon">
            <img src="@/assets/curse-logo.svg" width="20" alt="" />
          </div>
          <p>Curse Import</p>
          <small>Import a CurseForge .zip file</small>
        </div>
      </div>
    </div>
    
    <create-instance :open="createOpen" @close="createOpen = false" />
    <import-instance :open="importOpen" @close="importOpen = false" />
    <share-code-instance :open="shareOpen" @close="shareOpen = false" />
    <curse-import-instance :open="curseOpen" @close="curseOpen = false" />
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import CreateInstance from '@/components/core/modpack/create/CreateInstance.vue';
import CurseImportInstance from '@/components/core/modpack/create/CurseImportInstance.vue';
import ImportInstance from '@/components/core/modpack/create/ImportInstance.vue';
import ShareCodeInstance from '@/components/core/modpack/create/ShareCodeInstance.vue';

@Component({
  components: {ImportInstance, CurseImportInstance, CreateInstance, ShareCodeInstance}
})
export default class SidebarCreate extends Vue {
  open = false
  
  createOpen = false;
  importOpen = false;
  shareOpen = false;
  curseOpen = false;
  
  mounted() {
    document.addEventListener('click', this.close)
  }
  
  destroyed() {
    document.removeEventListener('click', this.close)
  }  
  
  close(event: any) {
    if (event.target.closest('.create-instance-container')) return
    this.open = false
  }
}
</script>

<style scoped lang="scss">
.create-instance-container {
  position: relative;
  margin-top: 1rem;
  margin-bottom: .5rem;
  
  .action-icon {
    padding: .7rem 1rem;
    background-color: black;
    border-radius: 8px;
    cursor: pointer;
    
    transition: background-color .25s ease-in-out;
    
    &:hover {
      background-color: var(--color-success-button);
    }
  }
  
  .dropdown {
    position: absolute;
    left: calc(100% + 1.5rem);
    width: 365px;
    background-color: var(--color-navbar);
    border-radius: 8px;
    border: 1px solid rgba(white, .2);
    padding: .25rem 0;
    z-index: 200;
    top: 0;
    font-size: 14px;
    transform: translateX(-10px);
    opacity: 0;
    visibility: hidden;
    transition: transform .25s ease-in-out, opacity .25s ease-in-out, visibility .25s ease-in-out;
    
    &.active {
      transform: translateX(0);
      opacity: 1;
      visibility: visible;
    }
    
    .options {
      display: flex;
      flex-direction: column;
      gap: .25rem;
      
      .separator {
        height: 1px;
        background-color: rgba(white, .2);
      }
      
      .option {
        display: grid;
        grid-template: 'icon content' 
                       'icon content2';
        
        align-items: center;
        
        grid-template-columns: auto 1fr;
        gap: 0 1rem;
        padding: .75rem;
        margin: 0 .25rem;
        cursor: pointer;
        border-radius: 6px;
        
        transition: background-color .25s ease-in-out;

        &.create:hover { background-color: hsl(190deg 73% 58% / 15%); }
        &.import:hover { background-color: hsl(100deg 73% 58% / 15%); }
        &.share:hover { background-color: hsl(290deg 73% 58% / 15%); }
        &.curse:hover { background-color: hsl(var(--curse-color-hsl) / 15%); }
        
        .icon {
          --size: 2.5rem;
          
          grid-area: icon;
          background-color: black;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          width: var(--size);
          height: var(--size);
          border: 1px solid rgba(white, .2)
        }
        
        &.create .icon { background-color: hsl(190deg 73% 58% / 35%); }
        &.import .icon { background-color: hsl(100deg 73% 58% / 35%); }
        &.share .icon { background-color: hsl(290deg 73% 58% / 35%); }
        &.curse .icon { background-color: var(--curse-color); }
        
        p {
          grid-area: content;
          font-weight: bold;
        }
        
        small {
          grid-area: content2;
          opacity: .8;
          font-size: 0.875em;
        }
      }
    }
  }
}
</style>