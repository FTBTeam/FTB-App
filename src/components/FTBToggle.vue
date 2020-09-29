<template>
    <div :class="`flex flex-${inline ? 'row' : 'col'} ${inline ? 'items-center' : ''} my-2`">
        <template v-if="!inline">
            <div class="w-full px-1" >
                <label class="block uppercase tracking-wide text-xs font-bold mb-2" :class="`${disabled ? 'text-gray-600' : ''}`">
                    {{label}}
                </label>
                <span class="toggle border border-input flex items-center rounded" style="width: 56px" :class="toggleClasses" @click="toggle">
                    <span class="w-6 h-6 shadow-inner bg-input cursor-pointer rounded-sm bg-gray-400" :class="`${disabled ? 'cursor-not-allowed' : ' cursor-pointer '}`"></span>
                </span>
            </div>
        </template>
        <template v-else>
                <span class="toggle border border-input flex items-center rounded" style="width: 56px" :class="toggleClasses" @click="toggle">
                    <span class="w-6 h-6 shadow-inner bg-input rounded-sm bg-gray-400" :class="`${disabled ? 'cursor-not-allowed' : ' cursor-pointer '}`"></span>
                </span>
                <label class="block uppercase tracking-wide text-xs font-bold mx-2" :class="`${disabled ? 'text-gray-600' : ''}`">
                    {{label}}
                </label>
        </template>
    </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
@Component({
    props: [
        'label',
        'onColor',
        'offColor',
        'value',
        'inline',
        'disabled'
    ],
})
export default class FTBToggle extends Vue {
    @Prop({default: ''})
    public label!: string;
    @Prop({default: 'bg-primary'})
    public onColor!: string;
    @Prop({default: ''})
    public offColor!: string;
    @Prop({default: false})
    public value!: boolean;
    @Prop({default: false})
    public inline!: boolean;
    @Prop({default: false})
    public disabled!: boolean;
    public toggle() {
        if(this.disabled){
            return;
        }
        this.handleChange(!this.value);
    }

    get toggleClasses() {
        const currentColor = this.currentColor;
        return [
            currentColor,
            this.value ? 'justify-end' : 'justify-start',
            this.disabled ? 'cursor-not-allowed' : 'cursor-pointer'
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

<style lang="scss">

.toggle {
    transition: all .5s;
}

</style>
