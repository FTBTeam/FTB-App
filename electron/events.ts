import { ipcMain } from 'electron';
import os from 'os';

ipcMain.handle("os/platform", async () => {
  switch (os.type()) {
    case 'Darwin':
      return 'mac';
    case 'Linux':
      return 'linux';
    default:
      return 'windows';
  }
})

ipcMain.handle("os/arch", async () => {
  return os.arch();
});