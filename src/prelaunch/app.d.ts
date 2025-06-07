import { UpdateFileInfo } from 'builder-util-runtime/out/updateInfo';

export interface ProgressInfo {
  total: number;
  delta: number;
  transferred: number;
  percent: number;
  bytesPerSecond: number;
}