<template>
    <div class="w-full">
        <ul class="subNav">
            <li v-for="item in subItems" class="pl-10 pb-2 cursor-pointer" v-bind:class="{'bg-sidebar-item-active': isActive(item.link)}" @click="goTo(item.link)"><font-awesome-icon :icon="item.icon" class="mr-3" />{{item.name}}</li>
        </ul>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    props: [
        'isActive',
        'subItems',
    ],
})
export default class SubNavItem extends Vue {
    public goTo(page: string): void {
        this.$router.push(page).catch((err) => { return; });
    }
}
</script>

<style lang="scss">
    .subNav li{
        padding-top: 10px;
    }
</style>
