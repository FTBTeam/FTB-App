import { defineStore } from 'pinia';
import { OpenModalData } from '@/core/types/javaApi';

type ModalStore = {
  modal: OpenModalData | null;
}

export const useModalStore = defineStore("modals", {
  state: (): ModalStore => {
    return {
      modal: null,
    }
  },

  actions: {
    openModal(modal: OpenModalData) {
      this.modal = modal;
    },
    closeModal() {
      this.modal = null;
    },
  }
})