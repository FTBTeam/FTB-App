import { defineStore } from 'pinia';
import { DialogForm } from '@/core/controllers/dialogsController.ts';

export type Dialog = {
  title: string;
  subTitle?: string;
  type?: "error" | "warning" | "info" | "success";
  content: string; // Markdown?
  form?: DialogForm;
  buttons?: DialogButton[],
  working?: boolean;
  onClose?: () => void;
}

export type DialogButton = {
  action: () => void;
  text: string;
  icon?: string;
  type?: "error" | "warning" | "info" | "success";
}


type DialogState = {
  dialogs: Dialog[]
}

export const useDialogsStore = defineStore("dialog", {
  state: (): DialogState => {
    return {
      dialogs: []
    }
  },

  actions: {
    openDialog(dialog: Dialog) {
      this.dialogs.push(dialog)
    },
    closeDialog(dialog: Dialog) {
      if (dialog.onClose) dialog.onClose();
      this.dialogs.splice(this.dialogs.indexOf(dialog), 1)
    },
    updateDialog(dialog: Dialog) {
      this.dialogs.splice(this.dialogs.indexOf(dialog), 1, dialog)
    }
  }
})