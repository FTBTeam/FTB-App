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
import { wsTimeoutWrapperTyped } from '@/utils';
import { Action } from 'vuex-class';
import { Instance } from '@/modules/modpacks/types';
import { RouterNames } from '@/router';

@Component
export default class DuplicateInstanceModal extends Vue {
  @Prop() uuid!: string;
  @Prop() instanceName!: string;
  @Action('showAlert') public showAlert: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;

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

    let result;
    try {
      result = await wsTimeoutWrapperTyped<any, { success: boolean; message: string; uuid: string }>({
        type: 'duplicateInstance',
        uuid: this.uuid,
        newName: this.newName,
      });
    } catch {
      this.showAlert({
        type: 'danger',
        title: 'Error',
        message: 'Unable to duplicate as no response was found for the request...',
      });

      this.working = false;

      return;
    }

    if (!result.success) {
      this.working = false;
      this.showAlert({
        type: 'danger',
        title: 'Error',
        message: result.message,
      });
      return;
    }

    this.status = 'Refreshing modpacks';
    const refreshResult = await wsTimeoutWrapperTyped<any, Instance[]>({ type: 'installedInstances' });
    this.storePacks(refreshResult);

    this.status = '';
    this.done = false;
    this.working = false;
    this.newName = '';
    this.$emit('finished');

    this.$router.push({ name: RouterNames.ROOT_LOCAL_PACK, query: { uuid: result.uuid } }).catch(() => {});
  }
}
</script>

<style scoped lang="scss"></style>
