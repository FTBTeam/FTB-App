<template>
    <div class="bg-sidebar-item p-5 rounded my-4">
        <a @click="openExternal" :href="link" class="text-2xl leading-none cursor-pointer hover:underline">{{title}}</a>
        <br><small class="text-sm text-gray-400">{{date}}</small>
        <p class="text-gray-300">
            <slot></slot>
        </p>
        <p><a @click="openExternal" :href="link" class="text-gray-400 hover:text-white leading-none cursor-pointer hover:underline">Continue Reading...</a></p>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    name: 'ftb-news',
    props: [
        'title',
        'date',
        'content',
        'link',
    ],
})
export default class FTBNews extends Vue {
}
</script>

<style lang="scss">

</style>
