<script lang="ts" setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

const {
  placeholder = 'Search',
  min = -1,
  darkMode = false,
  alpha = false,
} = defineProps<{
  placeholder?: string;
  min?: number;
  darkMode?: boolean;
  alpha?: boolean;
}>();

const value = defineModel<string>()

const emit = defineEmits<{
  (e: 'input', value: string): void;
}>();
</script>

<template>
  <div
    class="flex flex-row items-center relative shadow rounded ftb-search"
    :class="{ darker: darkMode, alpha, nope: min > 0 && value.length > 0 && value.length < min }"
  >
    <input
      class="block w-full p-3 rounded-tl rounded-bl appearance-none leading-normal text-gray-300"
      :value="value"
      :placeholder="placeholder"
      @focusout="emit('input', value)"
      @keydown.enter="emit('input', value)"
      @input="emit('input', value)"
    />
    <div class="search-button p-3 cursor-pointer rounded-tr rounded-br">
      <FontAwesomeIcon icon="search" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.shadow-error {
  box-shadow: 0 1px 3px 0 rgba(199, 13, 13, 0.74), 0 1px 2px 0 rgba(199, 13, 13, 0.74);
}

.ftb-search {
  input,
  .search-button {
    background: var(--color-navbar);
  }

  &.darker {
    input,
    .search-button {
      background-color: var(--color-background);
    }
  }

  &.alpha {
    input,
    .search-button {
      background-color: rgba(black, 0.4);
    }
  }

  &.nope {
    box-shadow: 0 0 15px rgba(253, 55, 29, 0.329);
  }
}
</style>
