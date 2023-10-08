<template>
  <div :class="`flex flex-${inline ? 'row' : 'col'} ${inline ? 'items-center' : ''} my-2 ftb-toggle-area`" @click="toggle">
    <template v-if="!inline">
      <div class="w-full flex ftb-toggle">
        <div class="main flex-1 mr-8">
          <label class="block tracking-wide font-bold mb-2" :class="`${disabled ? 'opacity-50' : ''}`">
            {{ label }}
          </label>
          <small v-if="small" class="text-muted max-w-xl block" :class="{ 'opacity-25': disabled }">{{ small }}</small>
        </div>
        <span
          class="toggle border border-input flex items-center rounded bg-black"
          style="width: 56px"
          :class="`${toggleClasses.join(' ')} ${disabled ? 'opacity-25' : ''}`"
        >
          <span
            class="w-6 h-6 shadow-inner cursor-pointer rounded-sm bg-white"
            :class="`${disabled ? 'cursor-not-allowed' : ' cursor-pointer '}`"
          ></span>
        </span>
      </div>
    </template>
    <template v-else>
      <span
        class="toggle border border-input flex items-center rounded"
        style="width: 56px"
        :class="toggleClasses"
      >
        <span
          class="w-6 h-6 shadow-inner bg-input rounded-sm bg-gray-400"
          :class="`${disabled ? 'cursor-not-allowed' : ' cursor-pointer '}`"
        ></span>
      </span>
      <label class="block uppercase tracking-wide text-xs font-bold ml-4" :class="`${disabled ? 'opacity-25' : ''}`">
        {{ label }}
      </label>
    </template>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FTBToggle extends Vue {
  @Prop({ default: '' }) label!: string;
  @Prop({ default: 'bg-primary' }) onColor!: string;
  @Prop({ default: '' }) offColor!: string;
  @Prop({ default: false }) value!: boolean;
  @Prop({ default: false }) inline!: boolean;
  @Prop({ default: false }) disabled!: boolean;
  @Prop({ default: '' }) small!: string;

  public toggle() {
    if (this.disabled) {
      return;
    }
    this.handleChange(!this.value);
  }

  get toggleClasses() {
    const currentColor = this.currentColor;
    return [
      currentColor,
      this.value ? 'justify-end' : 'justify-start',
      this.disabled ? 'cursor-not-allowed' : 'cursor-pointer',
    ];
  }

  get currentColor() {
    return this.value ? this.onColor : this.offColor;
  }

  public handleChange(value: boolean) {
    this.$emit('change', value);
  }
}
</script>

<style lang="scss" scoped>
.ftb-toggle-area {
  .toggle {
    transition: all 0.5s;
  }

  .ftb-toggle {
    align-items: center;
  }
}
</style>
