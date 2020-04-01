<template>
    <div :class="`ftb-button p-2 bg-${color}-button cursor-pointer`" @click="$emit('click')">
        <p class="text-xl uppercase cursor-pointer"><slot></slot></p>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    name: 'ftb-button',
    props: [
        'click',
        'color',
    ],
})
export default class FTBButton extends Vue {
}
</script>

<style lang="scss">
</style>
