import store from '@/modules/store';
import {Dialog, DialogButton} from '@/core/state/misc/dialogsState';
import {emitter} from '@/utils';

class DialogHolder {
  private readonly _dialog: Dialog;
  
  constructor(dialog: Dialog) {
    this._dialog = dialog;
  }
  
  setWorking(working: boolean) {
    this._dialog.working = working;
    store.dispatch('v2/dialogs/updateDialog', this._dialog);
  }
  
  close() {
    store.dispatch('v2/dialogs/closeDialog', this._dialog);
  }
  
  get dialog() {
    return this._dialog;
  }
}

class DialogsController {
  async createConfirmationDialog(title: string, message: string): Promise<boolean> {
    return new Promise((resolve) => {
      const holder = new DialogHolder(dialog(title)
        .withContent(message)
        .withCloseAction(() => {
          resolve(false);
        })
        .withType("warning")
        .withButton(button("Cancel")
          .withType("error")
          .withIcon("times")
          .withAction(() => {
            holder.close();
          })
          .build())
        .withButton(button("Confirm")
          .withType("success")
          .withIcon("check")
          .withAction(() => {
            resolve(true);
            holder.close(); // The close action will also do this but this will just make sure
          })
          .build())
        .build());

      store.dispatch('v2/dialogs/openDialog', holder.dialog);
    })
  }
  
  createDialog(dialog: Dialog) {
    store.dispatch('v2/dialogs/openDialog', dialog);
    return new DialogHolder(dialog);
  }
  
  createErrorDialog(message: string) {
    
  }
}

export const dialogsController = new DialogsController();

export class DialogBuilder {
  private subTitle?: string;
  private type?: "error" | "warning" | "info" | "success";
  private content?: string;
  
  private buttons?: DialogButton[];
  private onClose?: () => void;
  
  private constructor(
    private title: string,
  ) {}
  
  static create(title: string) {
    return new DialogBuilder(title);
  }
  
  withSubTitle(subTitle: string) {
    this.subTitle = subTitle;
    return this;
  }
  
  withType(type: "error" | "warning" | "info" | "success") {
    this.type = type;
    return this;
  }
  
  withContent(content: string) {
    this.content = content;
    return this;
  }
  
  withButton(button: DialogButton) {
    if (!this.buttons) this.buttons = [];
    this.buttons.push(button);
    return this;
  }
  
  withCloseAction(onClose: () => void) {
    this.onClose = onClose;
    return this;
  }
  
  build() {
    return {
      title: this.title,
      subTitle: this.subTitle,
      type: this.type,
      content: this.content,
      buttons: this.buttons,
      onClose: this.onClose,
    } as Dialog
  }
}

/**
 * TODO: Add support for basic forms via a context and validation system
 */
export class ButtonBuilder {
  action?: () => void;
  icon?: string;
  type?: "error" | "warning" | "info" | "success";
  
  private constructor(
    private text: string,
  ) {}
  
  static create(text: string) {
    return new ButtonBuilder(text);
  }
  
  withAction(action: () => void) {
    this.action = action;
    return this;
  }
  
  withIcon(icon: string) {
    this.icon = icon;
    return this;
  }
  
  withType(type: "error" | "warning" | "info" | "success") {
    this.type = type;
    return this;
  }
  
  build() {
    return {
      action: this.action,
      text: this.text,
      icon: this.icon,
      type: this.type,
    } as DialogButton
  }
}

export const dialog = (title: string) => DialogBuilder.create(title);
export const button = (text: string) => ButtonBuilder.create(text)