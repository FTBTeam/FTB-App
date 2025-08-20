<script setup lang="ts">
import {DialogForm} from "@/core/controllers/dialogsController.ts";
import {Input} from "@/components/ui";
import {onMounted} from "vue";
import TextArea from "@/components/ui/form/TextArea/TextArea.vue";
import UiSelect from "@/components/ui/select/UiSelect.vue";
import CategorySelector from "@/components/groups/modpack/create/CategorySelector.vue";
import {defaultInstanceCategory} from "@/core/constants.ts";

const {
  form,
  errors
} = defineProps<{
  form: DialogForm,
  errors: Record<string, string>
}>()

const model = defineModel<Record<string, string>>();

onMounted(() => {
  initData()
})

function initData() {
  if (!model.value) {
    model.value = {};
  }

  // Initialize model with empty values based on form fields
  form.fields.forEach(field => {
    model.value![field.name] = field.initialValue ?? '';
    if (field.type === 'category-select' && (!field.initialValue || field.initialValue === '')) {
      model.value![field.name] = defaultInstanceCategory;
    }
  });
}
</script>

<template>
  <div class="flex flex-col gap-4">
    <div v-for="(field, index) in form.fields" :key="index">
      <template v-if="model && typeof model[field.name] !== 'undefined'">        
        <Input
          fill
          :label="field.label"
          v-if="field.type === 'input' || field.type === 'password'"
          :is-password="field.type === 'password'"
          v-model="model[field.name]"
        />
        
        <TextArea
          fill
          :label="field.label"
          v-if="field.type === 'textarea'"
          v-model="model[field.name]"
        />
        
        <UiSelect
          fill
          v-if="field.type === 'select'"
          :options="field.options"
          :label="field.label"
          v-model="model[field.name]"
        />
        
        <CategorySelector 
          v-if="field.type === 'category-select'" 
          v-model="model[field.name]" 
          :label="field.name" 
        />
        
        <small class="text-red-400" v-if="typeof errors[field.name] !== 'undefined'">{{errors[field.name]}}</small>
      </template>
    </div>
  </div>
</template>

