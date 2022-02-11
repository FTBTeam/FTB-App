<template>
  <div class="flex flex-col min-w-1/2">
    <h1 class="text-2xl mb-2">{{ title }}</h1>
    <div>
      <VueShowdown :markdown="content" :extensions="['classMap', 'newLine']"></VueShowdown>
    </div>
    <div class="mt-4" v-if="type === 'okCancel'">
      <ftb-button
        class="py-2 px-16 text-center float-right"
        color="primary"
        css-class="text-center text-l"
        :disabled="loading"
        @click="okAction"
        ><font-awesome-icon icon="sync-alt" spin v-if="loading" size="1x" /> <span v-else>Ok</span></ftb-button
      >
      <ftb-button
        class="py-2 px-4 mx-2 text-center float-right"
        color="danger"
        css-class="text-center text-l"
        :disabled="loading"
        @click="cancelAction"
        ><font-awesome-icon icon="sync-alt" spin v-if="loading" size="1x" /> <span v-else>Cancel</span></ftb-button
      >
    </div>
    <div v-if="type === 'custom'" class="buttons">
      <ftb-button
        v-for="(button, index) in buttons"
        :key="index"
        class="py-2 px-4 mx-2 text-center float-right"
        :disabled="loading"
        :color="button.colour"
        css-class="text-center text-l"
        @click="sendMessage(button.message)"
        >{{ button.name }}</ftb-button
      >
    </div>
    <div v-if="type === 'okOnly'">
      <ftb-button
        class="py-2 px-4 mx-2 text-center float-right"
        color="primary"
        css-class="text-center text-l"
        :disabled="loading"
        @click="okAction"
        >Ok</ftb-button
      >
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Action } from 'vuex-class';

@Component({
  name: 'MessageModal',
  props: {
    title: String,
    content: String,
    type: String,
    okAction: Function,
    yesAction: Function,
    noAction: Function,
    cancelAction: Function,
    buttons: Array,
    modalID: String,
    loading: Boolean,
  },
})
export default class MessageModal extends Vue {
  @Action('sendMessage') public sendWSMessage: any;
  @Prop()
  private modalID!: string;

  public sendMessage(message: string) {
    this.sendWSMessage({ payload: { type: 'modalCallback', id: this.modalID, message }, callback: () => {} });
  }
}
</script>
<style></style>
