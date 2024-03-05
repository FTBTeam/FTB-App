<template>
  <div>
    <modal-body>
      Duplicating {{ instanceName }} will copy all of the contents of this pack to a new instance.

      <ftb-input
        :value="newName"
        v-model="newName"
        class="mt-4 mb-4"
        label="New instance name"
        :disabled="working || done"
      />
      
      <category-selector v-model="newCategory" />
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
import {Component, Prop, Vue} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import {RouterNames} from '@/router';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {ns} from '@/core/state/appState';
import {AddInstanceFunction} from '@/core/state/instances/instancesState';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {equalsIgnoreCase} from '@/utils/helpers/stringHelpers';

@Component({
  components: {CategorySelector}
})
export default class DuplicateInstanceModal extends Vue {
  @Prop() uuid!: string;
  @Prop() instanceName!: string;
  @Prop() category!: string;

  @Getter('instances', ns("v2/instances")) instances!: SugaredInstanceJson[];
  @Action('addInstance', ns("v2/instances")) addInstance!: AddInstanceFunction;
  
  newName = '';
  newCategory = '';

  working = false;
  done = false;
  status = '';

  mounted() {
    const duplicateCount = this.getDuplicateNameCount();
    if (duplicateCount > 1) {
      this.newName = this.instanceName + ' (copy ' + (duplicateCount + 1) + ')';
    } else {
      this.newName = this.instanceName + ' (copy)';
    }
    this.newCategory = this.category;
  }
  
  async duplicate() {
    if (this.working) {
      return;
    }

    this.working = true;
    this.status = 'Starting duplication';
    
    const result = await sendMessage("duplicateInstance", {
      uuid: this.uuid,
      newName: this.newName,
      category: this.newCategory
    })
    
    if (!result.success) {
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
  
  getDuplicateNameCount() {
    return this.instances.filter(i => equalsIgnoreCase(i.name, this.instanceName)).length
  }
}
</script>

<style scoped lang="scss"></style>
