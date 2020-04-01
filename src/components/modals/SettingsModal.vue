<template>
    <div class="flex flex-col min-w-1/2">
        <h1 class="text-4xl">{{packName}}<small class="pl-2 text-base">{{packVersion}}</small></h1>
        <div>
          <ftb-slider label="Memory" v-model="instance.memory" :currentValue="instance.memory" minValue="512" :maxValue="settingsState.hardware.totalMemory" 
                      unit="MB" />
          <ftb-input label="Custom Arguments" :value="instance.jvmargs" v-model="instance.jvmargs"/>
          <ftb-input label="Name" :value="instance.name" v-model="instance.name"/>
        </div>
        <div class="mt-4 flex flex-row justify-center items-center">
            <!-- <ftb-button color="danger" class="mx-2" @click="deleteInstance"><span v-if="!loading">Delete</span><font-awesome-icon icon="spinner" size="lg" spin v-else/></ftb-button> -->
            <ftb-button color="primary" class="mx-2" @click="saveSettings"><span v-if="!loading">Save</span><font-awesome-icon icon="spinner" size="lg"  spin v-else/></ftb-button>
        </div>
    </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import FTBInput from '@/components/FTBInput.vue';
import {State, Action} from 'vuex-class';
import { Instance } from '../../modules/modpacks/types';
import { SettingsState } from '../../modules/settings/types';

@Component({
    components: {
        'ftb-button': FTBButton,
        'ftb-slider': FTBSlider,
        'ftb-input': FTBInput,
    },
    name: 'SettingsModal',
    props: [
        'packName',
        'packVersion',
        'instance',
    ],
})
export default class SettingsModal extends Vue {
    @State('settings') public settingsState: SettingsState | undefined = undefined;
    @Action('saveInstance', {namespace: 'modpacks'}) public saveInstance: any;
    @Action('refreshPacks', {namespace: 'modpacks'}) public refreshPacks: any;
    @Action('sendMessage') public sendMessage: any;

    @Prop()
    public instance!: Instance;

    public loading: boolean = false;

    public async saveSettings() {
        this.loading = true;
        await this.saveInstance(this.instance);
        this.loading = false;
        this.$emit('dismiss-modal');
    }

    public deleteInstance() {
        this.loading = true;
        this.sendMessage({payload: {type: 'uninstallInstance', uuid: this.instance.uuid}, callback: async (data: any) => {
            if (data.status === 'success') {
                await this.refreshPacks();
                this.$emit('dismiss-modal');
            }
        }});
    }
}
</script>
<style></style>
