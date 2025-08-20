import { Dialog, DialogButton, useDialogsStore } from '@/store/dialogStore.ts';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import { faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import {BasicUiSelectOption} from "@/components/ui/select/UiSelect.ts";
import {ZodSchema} from "zod";

export class DialogHolder {
  private readonly _dialog: Dialog;
  
  constructor(dialog: Dialog) {
    this._dialog = dialog;
  }
  
  setWorking(working: boolean) {
    this._dialog.working = working;
    const dialogStore = useDialogsStore();
    dialogStore.updateDialog(this._dialog)
  }
  
  close() {

    const dialogStore = useDialogsStore();
    dialogStore.closeDialog(this._dialog)
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
          .withIcon(faTimes)
          .withAction(() => {
            holder.close();
          })
          .build())
        .withButton(button("Confirm")
          .withType("success")
          .withIcon(faCheck)
          .withAction(() => {
            resolve(true);
            holder.close(); // The close action will also do this but this will just make sure
          })
          .build())
        .build());

      const dialogStore = useDialogsStore();
      dialogStore.openDialog(holder.dialog)
    })
  }
  
  createDialog(dialog: Dialog) {
    const dialogStore = useDialogsStore();
    dialogStore.openDialog(dialog)
    return new DialogHolder(dialog);
  }
}

export const dialogsController = new DialogsController();

export class DialogBuilder {
  private subTitle?: string;
  private type?: "error" | "warning" | "info" | "success";
  private content?: string;
  private form?: DialogForm;
  
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
  
  withForm(form: DialogForm) {
    this.form = form;
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
      form: this.form,
    } as Dialog
  }
}

export type DialogForm = {
  fields: FormField[];
  validator: ZodSchema;
  onSubmit: FormSubmitHandler;
  closeOnSubmit: boolean
}

export type FormField = {
  name: string;
  label: string;
  initialValue?: string;
} & ({
  type: "input" | "password" | "textarea";
} | {
  type: "select";
  options: BasicUiSelectOption[];
} | {
  type: "category-select";
})

export type FormSubmitHandler = (values: Record<string, string>) => void;

export class FormBuilder {
  private fields: FormField[] = [];
  private closeOnSubmit = true;
  
  private constructor(
    private readonly validator: ZodSchema,
    private readonly onSubmit: FormSubmitHandler,
  ) {}
  
  static create(
    validator: ZodSchema,
    onSubmit: FormSubmitHandler
  ) {
    return new FormBuilder(validator, onSubmit);
  }

  public input(name: string, label: string, initialValue?: string) {
    return this._field("input", name, label, initialValue);
  }
  
  public categorySelect(name: string, label: string, initialValue?: string) {
    this.fields.push({
      name, label, type: "category-select", initialValue
    });
    
    return this;
  }
  
  public select(name: string, label: string, options: BasicUiSelectOption[], initialValue?: string) {
    this.fields.push({
      name, label, type: "select", options, initialValue, 
    });
    
    return this;
  }
  
  public password(name: string, label: string, initialValue?: string) {
    return this._field("password", name, label, initialValue)
  }
  
  public textarea(name: string, label: string, initialValue?: string) {
    return this._field("textarea", name, label, initialValue)
  }
  
  private _field(type: "input" | "password" | "textarea", name: string, label: string, initialValue?: string) {
    this.fields.push({
      name,
      label,
      type,
      initialValue: initialValue ?? "",
    });
    
    return this;
  }
  
  public withCloseOnSubmit(closeOnSubmit: boolean) {
    this.closeOnSubmit = closeOnSubmit;
    return this;
  }
  
  public build(): DialogForm {
    return {
      fields: this.fields,
      validator: this.validator,
      onSubmit: this.onSubmit,
      closeOnSubmit: this.closeOnSubmit,
    }
  }
}

export class ButtonBuilder {
  action?: () => void;
  icon?: IconDefinition;
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
  
  withIcon(icon: IconDefinition) {
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
export const form = (validator: ZodSchema, onSubmit: FormSubmitHandler) => FormBuilder.create(validator, onSubmit);