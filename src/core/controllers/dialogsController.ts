import store from '@/modules/store';
import {Dialog, DialogButton} from '@/core/state/misc/dialogsState';
import {emitter} from '@/utils';

class DialogHolder {
  private readonly dialog: Dialog;
  
  constructor(dialog: Dialog) {
    this.dialog = dialog;
  }
  
  setWorking(working: boolean) {
    this.dialog.working = working;
    store.dispatch('v2/dialogs/updateDialog', this.dialog);
  }
  
  close() {
    store.dispatch('v2/dialogs/closeDialog', this.dialog);
  }
}

class DialogsController {
  createConfirmationDialog(message: string, callback: (result: boolean) => void) {
    store.dispatch('v2/dialogs/openDialog', {
      
    } as Dialog);
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
  
  private buttons?: DialogButton[]
  
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
  
  build() {
    return {
      title: this.title,
      subTitle: this.subTitle,
      type: this.type,
      content: this.content,
      buttons: this.buttons,
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