<template>
 <div class="categorySelector">
   <div class="category-or-create mb-6">
     <div class="selection flex gap-2 items-end" v-if="!manualCategoryCreate">
       <selection2 :open-up="true" label="Category" :options="(categories ?? []).map(e => ({value: e, label: e}))" :value="internalValue" @input="v => emitInput(v)" class="flex-1" />
       <ftb-button color="info" class="px-4 py-3" @click="manualCategoryCreate = true"><font-awesome-icon class="mr-2" icon="plus" />Create new</ftb-button>
     </div>
     <div class="selection flex gap-2 items-end" v-else>
       <ftb-input label="Category name" :value="internalValue" @input="v => emitInput(v)" placeholder="My category" class="flex-1 mb-0" />
       <ftb-button color="warning" class="px-4 py-3" @click="() => {
              emitInput('Default')
              manualCategoryCreate = false;
            }"><font-awesome-icon icon="times" /></ftb-button>
     </div>
   </div>
 </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import {ns} from '@/core/state/appState';
import {Getter} from 'vuex-class';
import Selection2 from '@/components/atoms/input/Selection2.vue';

@Component({
  components: {Selection2}
})
export default class CategorySelector extends Vue {
  @Getter("categories", ns("v2/instances")) categories!: string[];

  @Prop() value!: string;
  @Emit("input") emitInput(value: string) {
    this.internalValue = value;
    return value;
  }
  
  manualCategoryCreate = false;
  internalValue = this.value;
  
  @Watch("value", {immediate: true})
  onValueChange(value: string) {
    if (value === this.internalValue) return;
    
    this.internalValue = value;
  }
}
</script>

<style lang="scss" scoped>

</style>