<template>
  <div>
    <modal-body>
      Duplicating {{ instanceName }} will copy all of the contents of this pack to a new instance. Are you sure this is
      want you want to do? If so, give your new instance a name!

      <ftb-input
        :value="newName"
        v-model="newName"
        class="mt-4"
        label="New instance name"
        :disabled="working || done"
      />
    </modal-body>
    <modal-footer class="flex justify-end">
      <ftb-button
        :disabled="working || done"
        class="py-2 px-8"
        color="primary"
        css-class="text-center text-l"
        @click="duplicate"
      >
        <template v-if="!working && !done">
          <font-awesome-icon icon="copy" class="mr-2" size="1x" />
          Duplicate
        </template>
        <template v-else>
          <font-awesome-icon icon="spinner" class="mr-2" spin size="1x" />
          {{ status }}
        </template>
      </ftb-button>
    </modal-footer>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import { RouterNames } from '@/router';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {ns} from '@/core/state/appState';
import {AddInstanceFunction} from '@/core/state/instances/instancesState';

@Component
export default class DuplicateInstanceModal extends Vue {
  @Prop() uuid!: string;
  @Prop() instanceName!: string;
  @Action('addInstance', ns("v2/instances")) addInstance!: AddInstanceFunction;
  
  newName = '';

  working = false;
  done = false;
  status = '';

  async duplicate() {
    if (this.working) {
      return;
    }

    this.working = true;
    this.status = 'Starting duplication';
    
    const result = await sendMessage("duplicateInstance", {
      uuid: this.uuid,
      newName: this.newName,
    })
    
    if (!result.success) {
      console.error(result.message)
      this.working = false;
      alertController.error(result.message)
      return;
    }
    
    this.status = 'Refreshing modpacks';
    await this.addInstance(result.instance)
    
    this.status = '';
    this.done = false;
    this.working = false;
    this.newName = '';
    
    await gobbleError(() => this.$router.push({ name: RouterNames.ROOT_LIBRARY }));
    this.$emit('finished');
  }
}
</script>

<style scoped lang="scss"></style>
