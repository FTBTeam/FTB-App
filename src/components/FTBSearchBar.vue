<template>
    <div class="w-full flex flex-col">
        <input class="bg-background focus:bg-background-lighten focus:outline-none border border-gray-700 block w-full p-2 appearance-none leading-normal text-gray-300" v-bind:value="value" :placeholder="placeholder" @keydown="doSearch" @focusout="doSearch" @input="$emit('input', $event.target.value)"/>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
@Component({
    props: [
        'placeholder',
        'doSearch',
        'value',
    ],
})
export default class FTBSearchBar extends Vue {
    @Prop()
    public buttonClick!: () => void;

    @Prop()
    public doSearch!: () => void;

    public handleClick() {
        this.buttonClick();
    }
}
</script>

<style lang="scss">

</style>