<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';
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
  platform.get.cb.copy(value);
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
  <div class="flex flex-col ftb-input" :class="{ disabled, 'my-2': !noSpacing }">
    <div class="w-full">
      <label class="block tracking-wide mb-2 font-bold" v-if="label">
        {{ label }}
      </label>
      <div class="flex flex-row w-full gap-4 items-center">
        <div class="input-area block flex-1">
          <input
            class="appearance-none block w-full ftb-btn bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded"
            :type="type"
            :placeholder="placeholder"
            :value="value"
            :disabled="disabled"
            :min="min"
            :max="max"
            @input="value = $event.target?.value"
            @blur="emit('blur')"
            @click="emit('click')"
          />
          <transition name="transition-fade">
            <div
              class="copy-btn bg-blue-700 hover:bg-blue-500 rounded px-3 py-1 text-sm cursor-pointer"
              v-show="value?.length > 0"
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
