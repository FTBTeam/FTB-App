<template>
  <div class="flex flex-col min-w-1/2">
    <h1 class="text-2xl mb-2">{{title}}</h1>
    <div>
      <VueShowdown :markdown="content" :extensions="['classMap', 'attribMap', 'newLine']"></VueShowdown>
<!--      <p>{{content}}</p>-->
    </div>
    <hr class="mt-4 mb-2">
    <div v-if="type==='okCancel'">
      <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="primary" css-class="text-center text-l" :disabled="loading" @click="okAction"><font-awesome-icon icon="sync-alt" spin v-if="loading" size="1x"/> <span v-else>Ok</span></ftb-button>
      <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="danger" css-class="text-center text-l" :disabled="loading" @click="cancelAction"><font-awesome-icon icon="sync-alt" spin v-if="loading" size="1x"/> <span v-else>Cancel</span></ftb-button>
<!--      <button-->
<!--          class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right"-->
<!--          @click="okAction"> Ok-->
<!--      </button>-->
<!--      <button-->
<!--          class="bg-red-500 hover:bg-red-400 text-white-600 font-bold py-2 px-4 mx-4 inline-flex items-center cursor-pointer float-right"-->
<!--          @click="cancelAction"> Cancel-->
<!--      </button>-->
    </div>
    <div v-if="type === 'custom'" class="buttons">
<!--      <button v-for="(button, index) in buttons" :key="index" class="text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right" @click="sendMessage(button.message)" :class="`bg-${button.colour}-500 hover:bg-${button.colour}-400`">-->
<!--        {{button.name}}-->
<!--      </button>-->
      <ftb-button v-for="(button, index) in buttons" :key="index" class="py-2 px-4 mx-2 text-center float-right" :disabled="loading" :color="button.colour" css-class="text-center text-l" @click="sendMessage(button.message)">{{button.name}}</ftb-button>
    </div>
    <div v-if="type==='okOnly'">
      <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="primary" css-class="text-center text-l" :disabled="loading" @click="okAction">Ok</ftb-button>
<!--      <button-->
<!--          class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right"-->
<!--          @click="okAction"> Ok-->
<!--      </button>-->
    </div>
  </div>
</template>
<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';
import {Button} from '@/types';
import { Action } from 'vuex-class';

@Component({
    name: 'MessageModal',
    components: {
        'ftb-button': FTBButton,
    },
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
        loading: Boolean
    },
})
export default class MessageModal extends Vue {
  @Action('sendMessage') public sendWSMessage: any;
  @Prop()
  private modalID!: string;

  public sendMessage(message: string) {
    this.sendWSMessage({payload: {type: 'modalCallback', id: this.modalID, message}, callback: () => {}});
  }
}
</script>
<style></style>
