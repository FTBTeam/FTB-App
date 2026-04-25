<script setup lang="ts" generic="T extends {key: string, value: string}">
import {computed, ref, useTemplateRef, watch} from "vue";
import {useAttachDomEvent} from "@/composables";
import {autoUpdate, offset, flip, hide, shift, size, useFloating} from "@floating-ui/vue";
import AbstractInput from "@/components/ui/form/AbstractInput.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faCheck, faChevronDown} from "@fortawesome/free-solid-svg-icons";
import {UiSelectProps} from "@/components/ui/select/UiSelect.ts";

const {
  label,
  disabled = false,
  fill = false,
  icon,
  placeholder = '',
  options,
  placement,
  minWidth = 180,
  multiple = false
} = defineProps<UiSelectProps<T> & {
  multiple?: boolean;
}>();

const value = defineModel<string | string[]>({
  default: null
});

const detailedValue = computed(() => {
  const modelValue = value.value;
  if (!modelValue) return null;
  if (!(modelValue instanceof Array)) {
    return options.find(option => option.key === modelValue) || null;
  }
  
  return options.filter(option => modelValue.includes(option.key));
})

const domRef = useTemplateRef('domRef');
const dropRef = useTemplateRef('dropRef');

const {floatingStyles, middlewareData} = useFloating(domRef, dropRef, {
  placement: placement ?? 'bottom-start',
  middleware: [offset(10), flip(), shift(), hide(), size()],
  whileElementsMounted: (reference, floating, update) => autoUpdate(reference, floating, update, {
    animationFrame: true
  })
});

const open = ref(false);

useAttachDomEvent('click', (event: MouseEvent) => {
  if (!open.value) return

  // Did we click on something outside the domRef?
  if (domRef.value) {
    const clickedInside = domRef.value.contains(event.target as Node) || (dropRef.value && dropRef.value.contains(event.target as Node));
    if (!clickedInside && open.value) {
      open.value = false;
    }
  }
})

watch(() => middlewareData.value.hide?.referenceHidden, (hidden) => {
  if (hidden && open.value) {
    open.value = false;
  }
});

function optionSelected(option: T) {
  if (!multiple) {
    value.value = option.key
    open.value = false;
  } else {
    if (!Array.isArray(value.value)) {
      value.value = [option.key]
    } else {
      if (value.value.includes(option.key)) {
        value.value = value.value.filter(e => e !== option.key);
      } else {
        value.value = [...value.value, option.key];
      }
    }
  }
}

function isOptionSelected(option: T) {
  if (Array.isArray(value.value)) {
    return value.value.some(e => e === option.key);
  } 
  
  return option.key === value.value;
}
</script>

<template>
  <div class="ui-select-3" ref="domRef">
    <AbstractInput :label="label" :disabled="disabled" :fill="fill" :icon="icon">
      <template v-slot:default="{ class: clazz }">
        <div :class="clazz" class="flex items-center justify-between" @click="open = !open">
          <div class="w-full">
            <span v-if="!detailedValue || (Array.isArray(detailedValue) && !detailedValue.length)">{{ placeholder || 'Select an option' }}</span>
            <span v-else-if="!$slots.option">{{ Array.isArray(detailedValue) ? detailedValue.map(e => e.value).join(", ") : detailedValue.value }}</span>
            <slot v-else :is-item="false" name="option" :clazz="''" :option="detailedValue"></slot>
          </div>

          <FontAwesomeIcon :icon="faChevronDown" class="ml-4 transition-transform" :class="{
          '-rotate-180': open  && !disabled,
        }"/>
        </div>
      </template>

      <template #suffix>
        <slot name="suffix"/>
      </template>
    </AbstractInput>

    <Teleport to="#root">
      <transition name="transition-fade">
        <div v-if="open && !disabled"
             class="z-[100000] flex flex-col gap-1 select-options-drop font-semibold bg-[#2a2a2a] max-h-[300px] overflow-auto p-2 w-fit border border-white/30 rounded-lg"
             ref="dropRef"
             :style="{
              ...floatingStyles,
              minWidth: `${minWidth}px`,
             }"
        >
          <div v-for="option in options" :key="option.key" @click="() => optionSelected(option)" class="rounded-lg cursor-pointer dark:text-white" :class="{ 'bg-green-400/30': isOptionSelected(option) }">
            <slot v-if="$slots.option" name="option" :is-selected="isOptionSelected(option)" :is-item="true" :clazz="`p-2 hover:bg-white/10 rounded-lg cursor-pointer`" :option="option"></slot>
            <div v-else class="p-2 pr-8 relative hover:bg-white/10 rounded-lg cursor-pointer">
              {{ option.value }}
              <div v-if="isOptionSelected(option)" class="absolute right-2 top-1/2 -translate-y-1/2 text-green-400">
                <FontAwesomeIcon :icon="faCheck" />
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>