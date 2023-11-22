<template>
  <div>
    <selection2 :open-up="!openDown" :label="label ?? 'Category'" :options="_options"
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
import Selection2, {SelectionOptions} from '@/components/core/ui/Selection2.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {alertController} from '@/core/controllers/alertController';
import {stringListContainsIgnoreCase} from '@/utils/helpers/arrayHelpers';

@Component({
  components: {UiButton, Selection2}
})
export default class CategorySelector extends Vue {
  @Getter('categories', ns('v2/instances')) categories!: string[];

  @Prop() label!: string;
  @Prop() value!: string;
  @Prop() openDown!: boolean;

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
    if (this.extraCategory === "") return;
    if (stringListContainsIgnoreCase(this.categories, this.extraCategory)) {
      alertController.warning("A category with that name already exists.")
      return;
    }

    this.showCreate = false;
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

    // Always sort the Default option to the top and the add new at the bottom, then the rest alphabetically
    categories.sort((a, b) => {
      if (a.value === "Default") return -1;
      if (b.value === "Default") return 1;
      if (a.value === "_") return 1;
      if (b.value === "_") return -1;
      return a.value.localeCompare(b.value);
    });
    
    return categories;
  }
}
</script>

<style lang="scss" scoped>

</style>