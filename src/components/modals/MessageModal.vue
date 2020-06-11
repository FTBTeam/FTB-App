<template>
  <div class="flex flex-col min-w-1/2">
    <h1 class="text-3xl mb-2">{{title}}</h1>
    <div>
      <VueShowdown :markdown="content" :extensions="['classMap', 'attribMap', 'newLine']"></VueShowdown>
<!--      <p>{{content}}</p>-->
    </div>
    <hr class="mt-4 mb-2">
    <div v-if="type==='okCancel'">
      <button
          class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right"
          @click="okAction"> Ok
      </button>
      <button
          class="bg-red-500 hover:bg-red-400 text-white-600 font-bold py-2 px-4 mx-4 inline-flex items-center cursor-pointer float-right"
          @click="cancelAction"> Cancel
      </button>
    </div>
    <div v-if="type==='YesNo'">
      <button
              class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right"
              @click="yesAction"> Yes
      </button>
      <button
              class="bg-red-500 hover:bg-red-400 text-white-600 font-bold py-2 px-4 mx-4 inline-flex items-center cursor-pointer float-right"
              @click="noAction"> No
      </button>
    </div>
    <div v-if="type==='okOnly'">
      <button
          class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center cursor-pointer float-right"
          @click="okAction"> Ok
      </button>
    </div>
  </div>
</template>
<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';

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
    },
})
export default class MessageModal extends Vue {

}
</script>
<style></style>
