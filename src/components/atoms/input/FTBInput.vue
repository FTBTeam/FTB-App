<template>
  <div class="flex flex-col my-2">
    <!-- <div class="flex flex-row justify-center my-4">
        <input class="bg-background focus:bg-background-lighten focus:outline-none border border-gray-700 block w-full p-2 appep class="text-white w-64 pr-4 mx-auto">{{label}}</p>
        <arance-none leading-normal text-gray-300" v-on:input="$emit('input', $event.target.value)" :value="value" v-on:blur="$emit('blur')"/>
           </div> -->
    <div class="w-full">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2" v-if="label">
        {{ label }}
      </label>
      <div class="flex flex-row items-center">
        <div class="input-area block w-full">
          <input
            class="appearance-none block w-full ftb-btn bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded"
            :type="type"
            :placeholder="placeholder"
            :value="value"
            :disabled="disabled"
            @input="$emit('input', $event.target.value)"
            @blur="$emit('blur')"
          />
          <transition name="fade">
            <div
              class="copy-btn bg-blue-700 hover:bg-blue-500 rounded px-3 py-1 text-sm cursor-pointer"
              v-show="value.length > 0"
              v-if="copyable"
              @click="copy"
            >
              {{ !copied ? 'Copy' : 'Copied!' }}
            </div>
          </transition>
        </div>
        <ftb-button v-if="button" :color="buttonColor" @click="handleClick" class="py-2 px-4 rounded-l-none py-2">{{
          buttonText
        }}</ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class FTBInput extends Vue {
  @Prop() public buttonClick!: () => void;
  @Prop({ default: 'text' }) type!: string;
  @Prop() value!: string;
  @Prop({ default: false }) disabled!: boolean;
  @Prop({ default: '' }) placeholder!: string;
  @Prop() button!: boolean;
  @Prop({ default: 'Submit' }) buttonText!: string;
  @Prop() buttonColor!: string;
  @Prop() label!: string;

  @Prop({ default: false }) copyable!: boolean;

  copied = false;
  timoutRef?: number = undefined;

  public handleClick() {
    this.buttonClick();
  }

  copy() {
    platform.get.cb.copy(this.value);
    this.copied = true;
    this.timoutRef = setTimeout(() => (this.copied = false), 700) as any;
  }

  destroyed() {
    if (!this.timoutRef) {
      clearTimeout(this.timoutRef);
    }
  }
}
</script>

<style lang="scss" scoped>
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

    &.fade-enter-active,
    &.fade-leave-active {
      transition: opacity 0.5s;
    }
    &.fade-enter,
    &.fade-leave-to {
      opacity: 0;
    }
  }

  input[disabled] {
    color: rgba(white, 0.5);
  }
}
</style>
