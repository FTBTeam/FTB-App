<template>
      <div class="flex flex-col min-w-1/2">
    <h1 class="text-2xl mb-2">{{title}}</h1>
    <div>
     <p>{{message}}</p>
    </div>
    <hr class="mt-4 mb-2" />
    <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="primary" css-class="text-center text-l" @click="okAction">Yes</ftb-button>
    <ftb-button class="py-2 px-4 mx-2 text-center float-right" color="danger" css-class="text-center text-l" @click="cancelAction">No</ftb-button>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
    name: 'ConfirmationModal',
    props: [
        'title',
        'message',
        'yesAction',
        'noAction',
    ],
})
export default class ConfirmationModal extends Vue {
}
</script>
<style></style>
