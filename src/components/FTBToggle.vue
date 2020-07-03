<template>
    <!-- <div class="flex flex-row align-center items-center">
        <p class="text-white mx-4">{{label}}</p>
        <span class="toggle border rounded-full border-grey flex items-center cursor-pointer w-12" :class="toggleClasses" @click="toggle">
            <span class="rounded-full border w-6 h-6 border-grey shadow-inner bg-white shadow cursor-pointer">
            </span>
        </span>
    </div> -->

    <div class="flex flex-col my-2">
    <div class="w-full px-1">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
        {{label}}
      </label>
      <span class="toggle border border-grey flex items-center cursor-pointer" style="width: 56px" :class="toggleClasses" @click="toggle">
        <span class="border w-6 h-6 border-grey shadow-inner bg-white shadow cursor-pointer">
        </span>
    </span>
    </div>
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

    public toggle() {
        this.handleChange(!this.value);
    }

    get toggleClasses() {
        const currentColor = this.currentColor;
        return [
            currentColor,
            this.value ? 'justify-end' : 'justify-start',
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
