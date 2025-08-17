<script setup lang="ts">

import {faSpinner, faTimes} from "@fortawesome/free-solid-svg-icons";
import {parseMarkdown} from "@/utils";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import UiButton from "../../../ui/UiButton.vue";
import {Dialog} from "@/store/dialogStore.ts";
import DialogForm from "@/components/groups/global/dialog/DialogForm.vue";
import {ref, watch} from "vue";

const {
  dialog,
  depth = 0,
} = defineProps<{
  dialog: Dialog,
  depth?: number;
}>()

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const formValid = ref(false);
const formValues = ref<Record<string, string>>({});

const formErrors = ref<Record<string, string>>({});

watch(() => formValues, (newValues) => {
  if (!dialog.form) {
    return;
  }
  
  const safeParse = dialog.form.validator.safeParse(newValues.value);
  if (!safeParse.success) {
    const errorsPerPath: Record<string, string> = {};
    for (const issue of safeParse.error.issues) {
      for (const path of issue.path) {
        const key = path.toString();
        if (errorsPerPath[key]) {
          errorsPerPath[key] += `, ${issue.message}`;
        } else {
          errorsPerPath[key] = issue.message;
        }
      }
    }
    
    formErrors.value = errorsPerPath;
    formValid.value = false;
  } else {
    formErrors.value = {};
    formValid.value = true;
  }
}, {deep: true})

</script>

<template>
  <div class="modal-contents" :style="`
            z-index: ${(501 - depth)};
            transform: scale(${1 - ((depth) * .1)}) translateX(-${15 * (depth)}px);
            opacity: ${1 - (depth) * 0.2};`"
  >
    <div class="modal-header" :class="{'no-subtitle': !dialog.subTitle}">
      <div class="modal-heading">
        <div class="title">{{ dialog.title }}</div>
        <div class="subtitle" v-if="dialog.subTitle">{{ dialog.subTitle }}</div>
      </div>
      <div class="modal-closer" @click="emit('close')">
        <FontAwesomeIcon class="closer" :icon="faTimes" />
      </div>
    </div>

    <div class="modal-body relative">
      <div :class="{'pointer-events-none opacity-50': dialog.working}">
        <div class="wysiwyg" v-if="dialog.content" v-html="parseMarkdown(dialog.content)"></div>
        <div class="form" v-if="dialog.form">
          <DialogForm :form="dialog.form" :errors="formErrors" v-model="formValues" />
        </div>
      </div>

      <transition name="transition-fade" :duration="250" :key="dialog.working ? 'working' : 'not-working'">
        <div v-if="dialog.working" class="inset-0 backdrop-blur-xs absolute flex items-center justify-center text-lg font-bold">
          <div>
            <FontAwesomeIcon :icon="faSpinner" spin class="mr-2" />
            Working...
          </div>
        </div>
      </transition>
    </div>

    <div class="modal-footer">
      <div class="buttons">
        <UiButton v-for="(button, index) in dialog.buttons"
                  :working="dialog.working"
                  :key="index"
                  :type="button.type === 'error' ? 'danger' : button.type"
                  :icon="button.icon"
                  @click="button.action">
          {{ button.text }}
        </UiButton>

        <UiButton
          v-if="dialog.form"
          :working="dialog.working"
          :disabled="!formValid"
          type="success"
          @click="() => {
            dialog.form?.onSubmit(formValues)
            if (dialog.form?.closeOnSubmit) {
              emit('close');
            }
          }"
        >
          Submit
        </UiButton>
      </div>
    </div>
  </div>
</template>

<style lang="scss">
.dialog {
  transition: opacity .25s ease-in-out;
  opacity: 1;
  position: absolute;

  .modal-contents {
    transform-origin: left center;
    transition: transform .25s ease-in-out, opacity .25s ease-in-out;
  }

  .buttons {
    display: flex;
    gap: .25rem .5rem;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>

