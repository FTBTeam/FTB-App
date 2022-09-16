<template>
  <div class="modpack-backsup-tab">
    <div class="backup" v-for="(backup, index) in backups" :key="index">
      <div class="preview">
        <img v-if="backup.preview" :src="backup.preview" alt="" />
      </div>

      <div class="backup-meta pl-6 pr-8">
        <div class="about">
          <b class="font-bold">{{ backup.worldName }}</b>
          <span :title="(backup.createTime / 1000) | dayjsFull">{{ (backup.createTime / 1000) | dayjsFromNow }}</span>
        </div>
        <div class="meta">
          <div class="size font-bold">
            {{ prettyBytes(backup.size) }}
          </div>
          <div class="snapshot bg-orange-500 text-xs px-2 rounded mt-2" v-if="backup.snapshot">Snapshot</div>
        </div>
      </div>

      <ftb-button color="primary" class="py-2 px-4 text-center" @click="showRestore(backup)">Restore</ftb-button>
      <ftb-button color="danger" class="py-2 px-4 ml-4 text-center" @click="showDelete(backup)">
        <font-awesome-icon icon="trash" />
      </ftb-button>
    </div>

    <modal
      :open="restoreModalShowing"
      title="Restore backup"
      subTitle="Warning, this action could cause data loss"
      @closed="modalClose"
    >
      <template v-if="actioningBackup">
        Are you sure you want to replace your current 'saves' folder with a backup from
        {{ (actioningBackup.createTime / 1000) | dayjsFull }}. Doing this may cause you to loose progression... be sure
        this is want you want to do.
      </template>
      <template #footer v-if="actioningBackup">
        <div class="text-right">
          <ftb-button color="warning" class="py-2 px-4 inline-block text-center" @click="restoreBackup">
            <template v-if="!actionRunning"> Restore backup </template>
            <font-awesome-icon icon="spinner" spin v-else />
          </ftb-button>
        </div>
      </template>
    </modal>

    <modal :open="deleteModalShowing" title="Delete backup" subTitle="Are you sure?" @closed="modalClose">
      <template v-if="actioningBackup">
        Are you sure you want to remove the backup for {{ actioningBackup.worldName }} that was created at
        {{ (actioningBackup.createTime / 1000) | dayjsFull }}. Doing so will remove any ability to backup your instance
        to this previous version of your world.
      </template>
      <template #footer v-if="actioningBackup">
        <div class="text-right">
          <ftb-button
            color="danger"
            class="py-2 px-4 inline-block text-center"
            @click="deleteBackup"
            :disabled="actionRunning"
          >
            <template v-if="!actionRunning">
              <font-awesome-icon class="mr-2" icon="trash" />
              Delete backup
            </template>
            <font-awesome-icon icon="spinner" spin v-else />
          </ftb-button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import {
  InstanceBackup,
  InstanceDeleteBackupRequest,
  InstanceRestoreBackupReply,
  InstanceRestoreBackupRequest,
} from '@/typings/subprocess/instanceBackups';
import { prettyByteFormat, wsTimeoutWrapperTyped } from '@/utils';
import { Action } from 'vuex-class';
import { Instance } from '@/modules/modpacks/types';

@Component
export default class ModpackBackups extends Vue {
  @Action('showAlert') public showAlert: any;

  @Prop() instance!: Instance;
  @Prop({ default: [] }) backups!: InstanceBackup[];

  prettyBytes = prettyByteFormat;

  restoreModalShowing = false;
  deleteModalShowing = false;

  actioningBackup: InstanceBackup | null = null;
  actionRunning = false;

  showRestore(backup: InstanceBackup) {
    this.actioningBackup = backup;
    this.restoreModalShowing = true;
  }

  showDelete(backup: InstanceBackup) {
    this.actioningBackup = backup;
    this.deleteModalShowing = true;
  }

  modalClose() {
    this.deleteModalShowing = false;
    this.restoreModalShowing = false;
    this.actioningBackup = null;
  }

  async restoreBackup() {
    if (this.actionRunning) {
      return;
    }

    this.actionRunning = true;
    const restoreRequest = await wsTimeoutWrapperTyped<InstanceRestoreBackupRequest, InstanceRestoreBackupReply>({
      type: 'instanceRestoreBackup',
      uuid: this.instance.uuid,
      backupLocation: this.actioningBackup?.backupLocation ?? '',
    });

    this.actionRunning = false;

    if (!restoreRequest.success) {
      this.showAlert({
        title: 'Unable to restore backup',
        message: restoreRequest.message,
        type: 'danger',
      });
    } else {
      this.showAlert({
        title: 'Success',
        message: 'Successfully restored backup',
        type: 'primary',
      });
    }

    this.modalClose();
  }

  async deleteBackup() {
    if (this.actionRunning) {
      return;
    }

    this.actionRunning = true;
    // Restore reply is the same as delete
    const deleteRequest = await wsTimeoutWrapperTyped<InstanceDeleteBackupRequest, InstanceRestoreBackupReply>({
      type: 'instanceDeleteBackup',
      backupLocation: this.actioningBackup?.backupLocation ?? '',
    });

    this.actionRunning = false;

    if (!deleteRequest.success) {
      this.showAlert({
        title: 'Unable to delete backup',
        message: deleteRequest.message,
        type: 'danger',
      });
    } else {
      this.showAlert({
        title: 'Success',
        message: 'Successfully deleted backup',
        type: 'primary',
      });

      this.$emit('backupsChanged');
    }

    this.modalClose();
  }
}
</script>

<style lang="scss">
.modpack-backsup-tab {
  .backup {
    display: flex;
    align-items: center;
    margin-bottom: 2rem;

    .preview {
      border-radius: 5px;
      background: black url('../../../assets/images/backup-preview-background.png') repeat;
      background-size: 8px;

      img {
        border-radius: 5px;
        width: 80px;
      }
    }

    .backup-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex: 1;

      .meta > *,
      .about > * {
        display: block;
      }

      .meta {
        text-align: right;

        .snapshot {
          padding-top: 0.2em;
          padding-bottom: 0.2em;
        }
      }
    }
  }
}
</style>
