import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {DialogForm} from '@/core/controllers/dialogsController';

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

export type DialogsState = typeof state;

const state = {
  dialogs: [] as any[],
  state: {}
}

const actions: ActionTree<DialogsState, RootState> = {
  async openDialog({ commit}, dialog: Dialog) {
    commit('OPEN_DIALOG', dialog);
  },
  async closeDialog({ commit}, dialog: Dialog) {
    commit('CLOSE_DIALOG', dialog);
  },
  async updateDialog({ commit}, dialog: Dialog) {
    commit('UPDATE_DIALOG', dialog);
  }
}

const mutations: MutationTree<DialogsState> = {
  OPEN_DIALOG: (state: DialogsState, dialog: Dialog) => state.dialogs.push(dialog),
  CLOSE_DIALOG: (state: DialogsState, dialog: Dialog) => {
    if (dialog.onClose) dialog.onClose();
    state.dialogs.splice(state.dialogs.indexOf(dialog), 1)
  },
  UPDATE_DIALOG: (state: DialogsState, dialog: Dialog) => state.dialogs.splice(state.dialogs.indexOf(dialog), 1, dialog),
}

const getters: GetterTree<DialogsState, RootState> = {
  dialogs: (state: DialogsState): Dialog[] => state.dialogs,
}

export const dialogsState: Module<DialogsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}