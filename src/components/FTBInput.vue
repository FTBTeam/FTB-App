<template>
    <div class="flex flex-col my-2">
      <!-- <div class="flex flex-row justify-center my-4">
        <input class="bg-background focus:bg-background-lighten focus:outline-none border border-gray-700 block w-full p-2 appep class="text-white w-64 pr-4 mx-auto">{{label}}</p>
        <arance-none leading-normal text-gray-300" v-on:input="$emit('input', $event.target.value)" :value="value" v-on:blur="$emit('blur')"/>
           </div> -->
    <div class="w-full px-1">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
        {{label}}
      </label>
      <div class="flex flex-row items-center">
        <input class="appearance-none block w-full bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded" type="text" :placeholder="placeholder" v-on:input="$emit('input', $event.target.value)" :value="value" v-on:blur="$emit('blur')"/>
        <ftb-button v-if="button" :color="buttonColor" @click="handleClick" class="py-2 px-4 rounded-l-none py-2">{{buttonText}}</ftb-button>
      </div>
    </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from './FTBButton.vue';
@Component({
    components: {
        'ftb-button': FTBButton,
    },
    props: [
        'label',
        'placeholder',
        'button',
        'buttonText',
        'buttonColor',
        'buttonClick',
        'value',
    ],
})
export default class FTBInput extends Vue {
    @Prop()
    public buttonClick!: () => void;

    public handleClick() {
        this.buttonClick();
    }
}
</script>

<style lang="scss">
.bg-input {
background-color: #252525;
}
.border-input {
    border-color: #1b1b1b;
}

</style>
