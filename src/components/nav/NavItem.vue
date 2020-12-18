<template>
    <div>
        <div class="flex flex-1 w-100 flex-row p-3 bg-sidebar-item hover:bg-sidebar-item-active cursor-pointer" v-bind:class="{'bg-sidebar-item-active': isActive, 'text-gray-700': disabled}" @click="$emit('click')">
            <slot></slot>
        </div>
        <div class="flex flex-1 w-100 flex-row pt-0 bg-sidebar-item cursor-pointer" v-if="hasSub">
            <SubNavItem :subItems="modpackSubnav" :isActive="isActiveFunc"></SubNavItem>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import SubNavItem from '@/components/nav/SubNavItem.vue';

@Component({
    props: [
        'isActive',
        'hasSub',
        'disabled',
    ],
    components: {
        SubNavItem,
    },
})
export default class NavItem extends Vue {
    public isActiveFunc(page: string): boolean {
        return page === 'home' && this.$route.path === '/' ? true : this.$route.path.startsWith(`/${page}`);
    }
}
</script>

<style lang="scss">
</style>
