<template>
    <div class="flex flex-col min-w-1/2">
        <h1 class="text-4xl ">{{packName}}</h1>
        <div>
            <p>{{packDescription}}</p>
        </div>
        <div class="flex mt-4 justify-center relative">
            <div class="relative w-1/2 block">
                <select class="rounded-none bg-input focus:outline-none focus:shadow-outline border border-input px-4 w-full h-full appearance-none leading-normal text-gray-400" v-model="version">
                    <option v-for="version in versions" :value="version.id" :key="`${packName}_${version.id}`">{{version.name}}</option>
                </select>
                <div class="pointer-events-none absolute right-0 inset-y-0 flex items-center px-2 text-gray-400">
                    <svg class="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg>
                </div>
            </div>
            <FTBButton color="secondary" class="py-2 px-4 rounded-l-none" @click="install">Install</FTBButton>
        </div>
    </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';
import { Versions } from '../../modules/modpacks/types';

@Component({
    components: {
        FTBButton,
    },
    name: 'InstallModal',
    props: [
        'packName',
        'packDescription',
        'versions',
        'doInstall',
        'selectedVersion',
    ],
})
export default class InstallModal extends Vue {
    @Prop()
    private versions!: Versions[];
    @Prop()
    private doInstall!: (version: number) => {};
    @Prop()
    private selectedVersion!: number | null;

    private version: number = this.selectedVersion === null || this.selectedVersion === undefined ? this.versions[0].id : this.selectedVersion;

    public install(): void {
        this.doInstall(this.version);
    }

}
</script>
<style></style>
