<template>
  <div class="flex flex-col h-full w-full p-2">
      <h1 class="text-4xl ">Add Friend</h1>
      <div class=" ">
        <ftb-input label="Friend Code" v-model="friendCode" :value="friendCode" />
        <ftb-input label="Display Name" v-model="displayName" :value="displayName" />
        <ftb-button color="primary" class="text-center px-2 py-1" @click="submit">Add</ftb-button>
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { shell, ipcRenderer, clipboard } from 'electron';
import { State, Action } from 'vuex-class';
import store from '@/store';
import config from '@/config';
import { AuthState } from '../../modules/auth/types';
import FTBInput from '../FTBInput.vue';
import FTBButton from '../FTBButton.vue';

@Component({
    components: {
        'ftb-input': FTBInput,
        'ftb-button': FTBButton,
    },
})
export default class AddFriend extends Vue {
    @Action('submitFriendRequest', {namespace: 'auth'})
    private submitFriendRequest!: (payload: {friendCode: string, display: string}) => void;
  private friendCode: string = '';
  private displayName: string = '';

  public submit() {
      if (this.friendCode.length === 0 || this.displayName.length === 0) {
          return;
      }
      this.submitFriendRequest({friendCode: this.friendCode, display: this.displayName});
  }
}
</script>

<style>
</style>