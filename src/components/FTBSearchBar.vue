<template>
  <div
    class="w-full flex flex-row items-center relative shadow rounded ftb-search"
    :class="{ darker: darkMode, nope: min > 0 && value.length > 0 && value.length < min }"
  >
    <input
      class="block w-full p-3 rounded-tl rounded-bl appearance-none leading-normal text-gray-300"
      :value="value"
      :placeholder="placeholder"
      @focusout="emit"
      @keydown.enter="emit"
      @input="emit"
    />
    <div class="search-button p-3 cursor-pointer rounded-tr rounded-br">
      <font-awesome-icon icon="search" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FTBSearchBar extends Vue {
  @Prop() value!: string;
  @Prop() placeholder!: string;
  @Prop({ default: -1 }) min!: number;

  @Prop({ default: false }) darkMode!: boolean;

  emit(event: any) {
    this.$emit('input', event.target.value);
  }
}
</script>

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

  &.nope {
    box-shadow: 0 0 15px rgba(253, 55, 29, 0.329);
  }
}
</style>
