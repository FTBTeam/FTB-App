<script lang="ts" setup>
import appPlatform from '@platform';
import { onUnmounted, ref } from 'vue';

const {
  type = 'text',
  disabled = false,
  placeholder = '',
  label = undefined,
  noSpacing = false,
  min,
  max,
  copyable = false,
} = defineProps<{
  type?: string;
  disabled?: boolean;
  placeholder?: string;
  label?: string;
  noSpacing?: boolean;
  min?: number;
  max?: number;
  copyable?: boolean;
}>();

const emit = defineEmits<{
  (e: 'blur'): void;
  (e: 'click'): void;
}>()

const value = defineModel<string>();

const copied = ref(false);
const timoutRef = ref<number | undefined>(undefined);

function copy() {
  appPlatform.cb.copy("" + value);
  copied.value = true;
  timoutRef.value = setTimeout(() => (copied.value = false), 700) as any;
}

onUnmounted(() => {
  if (!timoutRef.value) {
    clearTimeout(timoutRef.value);
  }
})
</script>

<template>
  <div class="flex flex-col ftb-input w-full" :class="{ disabled, 'my-2': !noSpacing }">
    <label class="text-xs text-white/80 mb-2 block uppercase font-bold" v-if="label">
      {{ label }}
    </label>
    <div class="flex flex-row w-full gap-4 items-center">
      <div class="flex-1 bg-black border-white/40 rounded border px-3 py-1 relative">
        <input
          class="appearance-none w-full"
          :type="type"
          :placeholder="placeholder"
          :value="value"
          :disabled="disabled"
          :min="min"
          :max="max"
          v-model="value"
          @blur="emit('blur')"
          @click="emit('click')"
        />
        <transition name="transition-fade">
          <div
            class="copy-btn bg-blue-700 hover:bg-blue-500 rounded px-3 py-1 text-sm cursor-pointer"
            v-show="(value?.length ?? 0) > 0"
            v-if="copyable"
            @click="copy"
          >
            {{ !copied ? 'Copy' : 'Copied!' }}
          </div>
        </transition>
      </div>
      <slot name="extra"></slot>
    </div>
  </div>
</template>

<style lang="scss" scoped>
label {
  transition: opacity 0.25s ease-in-out;
}

.ftb-input {
  &.disabled {
    * {
      cursor: not-allowed !important;
    }

    label {
      opacity: 0.5;
    }
  }
}

.ftb-btn {
  &::placeholder {
    color: rgba(white, 0.2);
  }
}

.input-area {
  position: relative;
  .copy-btn {
    position: absolute;
    top: 50%;
    right: 0.5rem;
    transform: translateY(-50%);
  }

  input[disabled] {
    color: rgba(white, 0.5);
  }
}
</style>
