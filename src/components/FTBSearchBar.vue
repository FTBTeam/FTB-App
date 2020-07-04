<template>
    <div class="w-full flex flex-row items-center relative">
        <input :class="`bg-navbar focus:outline-none border block w-full text-2xl transition-all duration-200 ease-in-out p-2 rounded appearance-none leading-normal text-gray-300 ${value.length > 0 && value.length < 3 ? 'border-red-700 shadow-error' : 'border-background-lighten shadow'}`"  v-bind:value="value" :placeholder="placeholder" @keydown="doSearch" @focusout="doSearch" @input="$emit('input', $event.target.value)"/>
        <div class="absolute right-0 bg-background-lighten p-4 h-full">
            <font-awesome-icon icon="search" />
        </div>
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
.shadow-error {
    box-shadow: 0 1px 3px 0 rgba(199, 13, 13, 0.74), 0 1px 2px 0 rgba(199, 13, 13, 0.74);
}
</style>