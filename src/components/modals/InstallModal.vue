<template>
    <div class="flex flex-col min-w-1/2">
        <h1 class="text-4xl ">{{packName}}</h1>
        <div>
            <p>{{packDescription}}</p>
        </div>
        <div class="flex mt-4 justify-center">
            <select class="rounded-none bg-input focus:outline-none focus:shadow-outline border border-input px-4 block w-1/2 appearance-none leading-normal text-gray-400" v-model="version">
                <option v-for="version in versions" :value="version.id" :key="`${packName}_${version.id}`">{{version.name}}</option>
            </select>
            <FTBButton color="secondary" class="rounded-l-none" @click="install">Install</FTBButton>
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
    name: 'InformationModal',
    props: [
        'packName',
        'packDescription',
        'versions',
        'doInstall',
    ],
})
export default class InstallModal extends Vue {
    @Prop()
    private versions!: Versions[];
    @Prop()
    private doInstall!: (version: number) => {};

    private version: number = this.versions[0].id;

    public install(): void {
        this.doInstall(this.version);
    }

}
</script>
<style></style>
