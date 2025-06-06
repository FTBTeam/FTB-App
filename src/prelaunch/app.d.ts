import { ReleaseNoteInfo, UpdateFileInfo } from 'builder-util-runtime/out/updateInfo';

export interface ProgressInfo {
  total: number;
  delta: number;
  transferred: number;
  percent: number;
  bytesPerSecond: number;
}

export interface BasicUpdateInfo {
  readonly version: string;
  readonly files: Array<UpdateFileInfo>;
  releaseName?: string | null;
  releaseDate: string;
}