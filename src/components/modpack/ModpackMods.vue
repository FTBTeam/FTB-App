<template>
  <div class="modpack-mods">
    <div v-for="(file, index) in modlist" :key="index">
      <div class="flex flex-row my-4 items-center">
        <p :title="`Version ${file.version}`">{{ file.name.replace('.jar', '') }}</p>
        <div class="ml-auto">
          <span class="rounded mx-2 text-sm bg-gray-600 py-1 px-2 clean-font">{{
            prettyBytes(parseInt(file.size))
          }}</span>

          <!-- TODO: Add matching to sha1 hashes, this isn't valid. // color: isMatched ? 'green' : 'red' -->
          <!-- TODO:Lfind where sha1 data is stored and provide it in a copy action -->
          <span class="sha-check">
            <font-awesome-icon icon="check-circle" color="lightgreen" class="ml-2 mr-1" /> SHA1
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { prettyByteFormat } from '@/utils/helpers';
import { Action } from 'vuex-class';
import { Prop } from 'vue-property-decorator';
import { Instance } from '@/modules/modpacks/types';
import FindMods from '@/components/modpack/FindMods.vue';

@Component({
  components: {
    FindMods,
  },
})
export default class ModpackMods extends Vue {
  @Action('showAlert') public showAlert: any;
  @Action('sendMessage') public sendMessage!: any;

  @Prop() instance!: Instance;
  @Prop() modlist!: any[];
  @Prop() updatingModlist!: boolean;

  prettyBytes = prettyByteFormat;
}
</script>

<style lang="scss" scoped>
.clean-font {
  font-family: Arial, Helvetica, sans-serif;
}
</style>
