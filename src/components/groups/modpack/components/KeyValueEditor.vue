<template>
  <div class="keyValueEditor">
    <label class="block mb-1">{{label}}</label>
    <small class="block text-muted" v-if="sublabel">{{ sublabel }}</small>
    
    <div class="nono-table mt-2">
      <header class="flex gap-4 items-center">
        <div class="text-muted">Argument name</div>
        <div class="text-muted">Value (Optional)</div>
        <div></div>
      </header>
      
      <div class="body" v-if="variables.length">
        <div class="row" v-for="(variable, index) in variables" :class="{active: currentValue && currentIndex === index}" :key="index">
          <div class="">
            <span class="block border border-transparent entry" @dblclick="editValue(index)" v-if="!isEditing(index)">
              <i class="text-muted" v-if="variable.name === ''">UNSET</i>
              <template v-else>{{variable.name}}</template>
            </span>
            <input class="inline-input" placeholder="Key" v-if="currentValue && isEditing(index)" v-model="currentValue.name" />
          </div>
          <div class="">
            <span class="block border border-transparent entry" @dblclick="editValue(index)" v-if="!isEditing(index)">
              <i class="text-muted" v-if="variable.value  === ''">UNSET</i>
              <template v-else>{{variable.value}}</template>
            </span>
            <input class="inline-input" placeholder="Value (Optional)" v-if="currentValue && isEditing(index)" v-model="currentValue.value" />
          </div>
          <div class="flex gap-1 items-center actions">
            <ui-button size="small" v-if="!isEditing(index)" @click="editValue(index)" icon="edit" aria-label="Edit" />
            <ui-button size="small" type="danger" @click="removeValue(index)" v-if="!isEditing(index)" icon="trash" aria-label="Delete" />
            <ui-button size="small" type="success" @click="saveValue()" v-if="isEditing(index)" icon="save" aria-label="Save" />
            <ui-button size="small" type="normal" @click="cancelEdit()" v-if="isEditing(index)" icon="times" aria-label="Cancel" />
          </div>
        </div>
      </div>
      <div class="body" v-else>
        No values added currently.
      </div>
    </div>

    <div class="flex justify-end gap-2">
      <ui-button v-if="variables.length" class="mt-3" size="small" type="warning" @click="clearAll()" icon="trash">Clear all</ui-button>
      <ui-button class="mt-3" size="small" type="primary" @click="addNew()" icon="plus">Add new</ui-button>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import UiButton from '@/components/ui/UiButton.vue';

export type KV = {
  name: string;
  value: string;
}

@Component({
  components: {UiButton}
})
export default class KeyValueEditor extends Vue {
  @Prop() label!: string;
  @Prop() sublabel!: string;
  
  @Prop({ default: () => [] }) variables!: KV[];
  
  currentValue: KV | null = null;
  currentIndex = -1;

  editValue(index: any) {
    this.currentValue = this.variables[index]
    this.currentIndex = index;
  }
  
  saveValue() {
    this.$emit("input", this.variables)
    this.currentValue = null;
    this.currentIndex = -1;
  }
  
  cancelEdit() {
    if (!this.currentValue) {
      return;
    }
    
    if (this.variables[this.currentIndex].name === '') {
      this.variables.splice(this.currentIndex, 1);
    }
    
    this.currentValue = null;
    this.currentIndex = -1;
  }
  
  removeValue(index: any) {
    this.variables.splice(index, 1);
  }
  
  addNew() {
    const emptyIndex = this.variables.findIndex((v: any) => v.name === '' && v.value === '');
    if (emptyIndex !== -1) {
      this.editValue(emptyIndex);
      return;
    }
    
    this.variables.push({
      name: '',
      value: ''
    });
    
    this.editValue(this.variables.length - 1);
  }
  
  clearAll() {
    this.variables = [];
  }
  
  isEditing(index: any) {
    return this.currentValue && this.currentIndex === index;
  }
}
</script>

<style lang="scss" scoped>
.nono-table {
  header, .row {
    display: grid;
    grid-template-columns: 1fr 1fr .35fr;
    gap: 1rem;
  }
  
  header {
    padding: .6rem .5rem;
    background-color: rgba(white, .1);
    border-radius: 5px 5px 0 0;
  }
  
  .body {
    display: flex;
    flex-direction: column;
    gap: .3rem;
    background-color: rgba(white, .05);
    border-radius: 0 0 5px 5px;
    padding: .5rem;
    
    .inline-input {
      border: none;
      background-color: rgba(white, .1);
      padding: .3rem .4rem;
      width: 100%;
      border-radius: 3px;
    }
    
    .entry {
      padding: 0.23rem 0;
      word-break: break-all;
    }
    
    .row {
      .actions {
        opacity: 0;
        transition: opacity .2s ease-in-out;
        justify-content: flex-end;
      }

      &:hover {
        .actions {
          opacity: 1;
        }
      }
    }
    
    .row {
      &:not(:last-child) {
        padding-bottom: .3rem;
        border-bottom: 1px solid rgba(white, .05);
      }
      
      &.active {
        .actions {
          opacity: 1;
        }
      }
    }
  }
}
</style>