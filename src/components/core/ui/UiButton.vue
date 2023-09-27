<template>
  <div class="ui-button" @click="click">
    <font-awesome-icon v-if="icon" :icon="icon" class="mr-2" />
    <slot />
  </div>
</template>

<script lang="ts">
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';

type ButtonType = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info';

@Component
export default class UiButton extends Vue {
  @Prop() type!: ButtonType;
  @Prop() disabled!: boolean;
  @Prop() working!: boolean;
  @Prop() icon!: IconDefinition;
  
  @Emit() click(event: MouseEvent) {
    if (this.disabled) {
      return;
    }
    
    return event;
  }
}
</script>

<style scoped lang="scss">

</style>