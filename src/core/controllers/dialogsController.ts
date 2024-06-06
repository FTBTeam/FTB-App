import store from '@/modules/store';
import {Dialog, DialogButton} from '@/core/state/misc/dialogsState';

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
  validator: FormValidator;
}

export type FormField = {
  name: string;
  label: string;
  type: SupportedFieldTypes;
  initialValue?: string;
  required?: boolean;
}

export type SupportedFieldTypes = "input" | "select" | "password" | "textarea"
export type FormValidator = (context: Record<string, any>) => [boolean, string[]];

export class FormBuilder {
  private required = true;
  private fields: FormField[] = [];
  
  private constructor(
    private readonly validator: FormValidator
  ) {}
  
  static create(
    validator: FormValidator,
  ) {
    return new FormBuilder(validator);
  }

  public input(name: string, label: string, initialValue?: string, required?: boolean) {
    return this._field("input", name, label, initialValue, required);
  }
  
  public select(name: string, label: string, initialValue?: string, required?: boolean) {
    return this._field("select", name, label, initialValue, required)
  }
  
  public password(name: string, label: string, initialValue?: string, required?: boolean) {
    return this._field("password", name, label, initialValue, required)
  }
  
  public textarea(name: string, label: string, initialValue?: string, required?: boolean) {
    return this._field("textarea", name, label, initialValue, required)
  }
  
  private _field(type: SupportedFieldTypes, name: string, label: string, initialValue?: string, required?: boolean) {
    this.fields.push({
      name,
      label,
      type,
      initialValue: initialValue ?? "",
      required: required ?? this.required,
    });
    
    return this;
  }
  
  public optional() {
    this.required = false;
    return this;
  }
  
  public build(): DialogForm {
    return {
      fields: this.fields,
      validator: this.validator,
    }
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