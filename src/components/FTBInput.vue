<template>
  <div class="flex flex-col my-2">
    <!-- <div class="flex flex-row justify-center my-4">
        <input class="bg-background focus:bg-background-lighten focus:outline-none border border-gray-700 block w-full p-2 appep class="text-white w-64 pr-4 mx-auto">{{label}}</p>
        <arance-none leading-normal text-gray-300" v-on:input="$emit('input', $event.target.value)" :value="value" v-on:blur="$emit('blur')"/>
           </div> -->
    <div class="w-full">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
        {{ label }}
      </label>
      <div class="flex flex-row items-center">
        <input
          class="appearance-none block w-full ftb-btn bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded"
          type="text"
          :placeholder="placeholder"
          :value="value"
          @input="$emit('input', $event.target.value)"
          @blur="$emit('blur')"
        />
        <ftb-button v-if="button" :color="buttonColor" @click="handleClick" class="py-2 px-4 rounded-l-none py-2">{{
          buttonText
        }}</ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  props: ['label', 'placeholder', 'button', 'buttonText', 'buttonColor', 'buttonClick', 'value'],
})
export default class FTBInput extends Vue {
  @Prop()
  public buttonClick!: () => void;

  public handleClick() {
    this.buttonClick();
  }
}
</script>

<style lang="scss" scoped>
.bg-input {
  background-color: #252525;
}
.border-input {
  border-color: #1b1b1b;
}
</style>

<style lang="scss" scoped>
.ftb-btn {
  &::placeholder {
    color: rgba(white, 0.2);
  }
}
</style>
