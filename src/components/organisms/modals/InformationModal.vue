<template>
    <div class="flex flex-col min-w-1/2">
        <h1 class="text-4xl ">{{packName}}<small class="pl-2 text-base">{{packVersion}}</small></h1>
        <div>
            <p>{{packDescription}}</p>
        </div>
    </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    name: 'InformationModal',
    props: [
        'packName',
        'packVersion',
        'packDescription',

    ],
})
export default class InformationModal extends Vue {
}
</script>
<style></style>
