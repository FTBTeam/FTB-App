export type InstanceBackup = {
  worldName: string;
  createTime: number;
  backupLocation: string;
  size: number;
  ratio: number;
  sha1: string;
  preview: string;
  snapshot: boolean;
};

export interface InstanceBackupsRequest {
  uuid: string;
  type: 'instanceGetBackups';
}

export interface InstanceBackupsReply extends InstanceBackupsRequest {
  backups: InstanceBackup[];
}

export interface InstanceDeleteBackupRequest {
  type: 'instanceDeleteBackup';
  backupLocation: string;
}

export interface InstanceRestoreBackupRequest {
  type: 'instanceRestoreBackup';
  backupLocation: string;
  uuid: string;
}

export interface InstanceRestoreBackupReply {
  message: string;
  success: boolean;
}
