<template>
      <div class="flex flex-col min-w-1/2">
    <h1 class="text-2xl mb-2">{{title}}</h1>
    <div>
     <p class="text-center">{{message}}</p>
    </div>
    <hr class="mt-4 mb-2" />
    <div class="flex justify-center">
      <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="danger" css-class="text-center text-l" @click="noAction">No</ftb-button>
      <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="primary" css-class="text-center text-l" @click="yesAction">Yes</ftb-button>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '../FTBButton.vue';

@Component({
    name: 'ConfirmationModal',
    props: [
        'title',
        'message',
        'yesAction',
        'noAction',
    ],
    components: {
      'ftb-button': FTBButton
    }
})
export default class ConfirmationModal extends Vue {
}
</script>
<style></style>
