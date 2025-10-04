import { defineStore } from 'pinia';
import { DialogForm } from '@/core/controllers/dialogsController.ts';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';

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
  icon?: IconDefinition;
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
      const index = this.dialogs.indexOf(dialog);
      if (index === -1) {
        console.warn("Dialog not found for update:", dialog.title);
        return;
      }
      
      const updatedDialogs = [...this.dialogs];
      updatedDialogs[index] = dialog;
      
      this.dialogs = updatedDialogs;
    }
  }
})