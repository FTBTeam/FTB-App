<template>
  <div>
    <selection2 :open-up="true" :label="label ?? 'Category'" :options="_options"
                :value="value" @input="v => valueChanged(v)" class="flex-1"/>
    
    <modal :open="showCreate" title="New category" @closed="showCreate = false">
      <ftb-input v-model="extraCategory" placeholder="FTB Packs" label="Category name" />
      <template #footer>
        <div class="flex justify-end">
          <ui-button @click="addCategory" type="success" icon="plus">Create</ui-button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import {ns} from '@/core/state/appState';
import {Getter} from 'vuex-class';
import Selection2, {SelectionOptions} from '@/components/atoms/input/Selection2.vue';
import UiButton from '@/components/core/ui/UiButton.vue';

@Component({
  components: {UiButton, Selection2}
})
export default class CategorySelector extends Vue {
  @Getter('categories', ns('v2/instances')) categories!: string[];

  @Prop() label!: string;
  @Prop() value!: string;

  @Emit('input') emitInput(value: string) {
    return value;
  }
  
  extraCategory = "";
  showCreate = false;

  valueChanged(value: string) {
    if (value === "_") {
      this.showCreate = true;
      return;
    }
    
    this.emitInput(value);
  }
  
  addCategory() {
    this.showCreate = false;
    if (this.extraCategory === "") return;
    
    this.emitInput(this.extraCategory)
  }
  
  get _options() {
    const categories: SelectionOptions = (this.categories ?? []).map(e => ({value: e, label: e}));
    if (this.extraCategory !== "") {
      categories.push({value: this.extraCategory, label: this.extraCategory});
      categories.push({
        value: "_", label: `Edit ${this.extraCategory}`, badge: {
          color: "#b46f2a",
          text: "+ Edit"
        }
      });
    } else {
      categories.push({
        value: "_", label: "Add new", badge: {
          color: "#2ab46e",
          text: "+ New"
        }
      });
    }
    return categories;
  }
}
</script>

<style lang="scss" scoped>

</style>