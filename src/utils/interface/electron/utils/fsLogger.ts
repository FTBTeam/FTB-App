// import path from 'path';
// import fs from 'fs';
import {electronAppHome} from '@/utils/interface/electron-helpers';

/**
 * Avoid use unless absolutely need!
 */
export class FSLogger {
  path: string;
  parent: string;

  constructor(name: string) {
    // this.parent = path.join(electronAppHome(), 'logs');
    // this.path = path.join(this.parent, `${name}.log`);

    this.ensureParentExists();
  }

  log(message: string, ...extraData: any[]) {
    const parsedExtraData = extraData
      .map(e => {
        // Handle errors
        if (e instanceof Error) {
          return JSON.stringify({
            message: e.message,
            stack: e.stack
          })
        }

        return JSON.stringify(e)
      })
      .join(" ");
    const outMessage = `${new Date().toISOString()} - ${message} ${parsedExtraData}\n`;

    try {
      this.ensureParentExists();

      // fs.appendFileSync(this.path, outMessage, {
      //   encoding: 'utf-8',
      // });
    } catch (e) {
      console.error("Failed to log message", e);
      console.debug("Original message", outMessage);
    }
  }

  clearLog() {
    // try {
    //   if (fs.existsSync(this.path)) {
    //     fs.writeFileSync(this.path, '');
    //   }
    // } catch (e) {
    //   console.error("Failed to clear log", e);
    // }
  }

  deleteLog() {
    // try {
    //   if (fs.existsSync(this.path)) {
    //     fs.rmSync(this.path);
    //   }
    // } catch (e) {
    //   console.error("Failed to delete log", e);
    // }
  }

  private ensureParentExists() {
    // try {
    //   if (!fs.existsSync(this.parent)) {
    //     fs.mkdirSync(this.parent, {
    //       recursive: true
    //     });
    //   }
    // } catch (e) {
    //   console.error("Failed to create log directory", e);
    // }
  }
}