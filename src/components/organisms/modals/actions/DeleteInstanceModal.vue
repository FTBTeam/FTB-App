<template>
  <div>
    <modal-body>
      Are you absolutely sure you want to delete <code>{{ this.instanceName }}</code
      >! Doing this <b class="font-black">WILL permanently</b> delete all mods, world saves, configurations, and all the
      rest... There is no way to recover this pack after deletion...
    </modal-body>
    <modal-footer class="flex justify-end">
      <ftb-button
        :disabled="working || done"
        class="py-2 px-8"
        color="danger"
        css-class="text-center text-l"
        @click="deleteInstance"
      >
        <template v-if="!working && !done && !askToConfirm">
          <font-awesome-icon icon="trash" class="mr-2" size="1x" />
          Delete
        </template>
        <template v-else-if="askToConfirm && !working && !done">
          <font-awesome-icon icon="triangle-exclamation" class="mr-2" size="1x" />
          Confirm deletion
        </template>
        <template v-else>
          <font-awesome-icon icon="spinner" class="mr-2" spin size="1x" v-if="!done" />
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
export default class DeleteInstanceModal extends Vue {
  @Prop() uuid!: string;
  @Prop() instanceName!: string;
  @Action('showAlert') public showAlert: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;

  working = false;
  done = false;
  askToConfirm = false;

  status = '';

  async deleteInstance() {
    if (!this.askToConfirm) {
      this.askToConfirm = true;
      return;
    }

    this.working = true;
    this.status = 'Deleting...';

    try {
      await wsTimeoutWrapperTyped<any, void>({
        type: 'uninstallInstance',
        uuid: this.uuid,
      });
    } catch {
      this.working = false;
      this.showAlert({
        type: 'danger',
        title: 'Error',
        message: 'Unable to delete instance... No response from agent...',
      });

      return;
    }

    const refreshResult = await wsTimeoutWrapperTyped<any, Instance[]>({ type: 'installedInstances' });
    this.storePacks(refreshResult);

    this.$router.push({ name: RouterNames.ROOT_LIBRARY }).catch(() => {});

    this.showAlert({
      type: 'primary',
      title: 'Success',
      message: `Successfully delete ${this.instanceName}`,
    });
  }
}
</script>

<style scoped lang="scss"></style>
