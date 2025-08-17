<script setup lang="ts" generic="T extends {key: string, value: string}">
import {computed, ref, useTemplateRef, watch} from "vue";
import {useAttachDomEvent} from "@/composables";
import {autoUpdate, offset, flip, hide, shift, size, useFloating} from "@floating-ui/vue";
import AbstractInput from "@/components/ui/form/AbstractInput.vue";
import {IconDefinition} from "@fortawesome/free-brands-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faChevronDown} from "@fortawesome/free-solid-svg-icons";
import {AlignedPlacement, Side} from "@floating-ui/utils";

const {
  label,
  disabled = false,
  fill = false,
  icon,
  placeholder = '',
  options,
  placement,
  minWidth = 180,
} = defineProps<{
  placement?: Side | AlignedPlacement
  label?: string;
  disabled?: boolean;
  fill?: boolean;
  icon?: IconDefinition;
  options: T[];
  placeholder?: string;
  minWidth?: number;
}>();

const value = defineModel<string>({
  default: null
});


const detailedValue = computed(() => {
  if (!value.value) return null;
  return options.find(option => option.key === value.value) || null;
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
</script>

<template>
  <div class="ui-select-3" ref="domRef">
    <AbstractInput :label="label" :disabled="disabled" :fill="fill" :icon="icon">
      <template v-slot:default="{ class: clazz }">
        <div :class="clazz" class="flex items-center justify-between" @click="open = !open">
          <div class="w-full">
            <span v-if="!detailedValue">{{ placeholder || 'Select an option' }}</span>
            <span v-else-if="!$slots.option">{{ detailedValue.value }}</span>
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

    <Teleport to="body">
      <transition name="transition-fade">
        <div v-if="open && !disabled"
             class="z-[60] select-options-drop font-semibold bg-[#2a2a2a] max-h-[300px] overflow-auto p-2 w-fit border border-white/30 rounded-lg"
             ref="dropRef"
             :style="{
              ...floatingStyles,
              minWidth: `${minWidth}px`,
             }"
        >
          <div v-for="option in options" :key="option.key" @click="() => {
            value = option.key
            open = false;
          }" class="cursor-pointer">
            <slot v-if="$slots.option" name="option" :is-item="true" :clazz="`p-2 hover:bg-white/10 rounded-lg cursor-pointer`" :option="option"></slot>
            <div v-else class="p-2 hover:bg-white/10 rounded-lg cursor-pointer">
              {{ option.value }}
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>