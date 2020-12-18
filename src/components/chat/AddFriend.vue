<template>
  <div class="flex flex-col h-full w-full p-2">
      <h1 class="text-4xl ">Add Friend</h1>
      <div class=" ">
        <div v-if="message.length > 0" :class="`p-4 bg-${messageType} text-white mt-2 rounded`">
          {{message}}
        </div>
        <ftb-input label="Friend Code" v-model="friendCode" :value="friendCode" />
        <ftb-button color="primary" class="text-center px-2 py-1" @click="submit" :disabled="auth.loading">Add</ftb-button>
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
import { FriendRequestResponse } from '../../modules/auth/actions';
import { shortenHash } from '../../utils';

@Component({
    components: {
        'ftb-input': FTBInput,
        'ftb-button': FTBButton,
    },
})
export default class AddFriend extends Vue {
  @State('auth')
  private auth!: AuthState;
  @Action('submitFriendRequest', {namespace: 'auth'})
  private submitFriendRequest!: (payload: {friendCode: string}) => Promise<FriendRequestResponse>;

  private message: string = '';
  private messageType: 'danger' | 'primary' = 'primary';
  private friendCode: string = '';

  public async submit() {
      this.message = '';
      if (this.friendCode.length === 0) {
          return;
      }
      if(this.friendCode === this.auth.token?.mc.friendCode){
        this.messageType = 'danger';
        this.message = "You cannot add yourself as a friend!";
        return;
      }
      const response: FriendRequestResponse = await this.submitFriendRequest({friendCode: this.friendCode});
      if (response.status === 'success') {
          this.message = 'Friend Request Sent';
          this.messageType = 'primary';
          this.friendCode = '';
          if (response.hash) {
            /// send friend request
            // ipcRenderer.send('sendFriendRequest', {target: shortenHash(response.hash), hash: response.hash, name: this.auth.token?.mc.display});
          }
      } else {
        this.messageType = 'danger';
        this.message = response.message;
      }
  }
}
</script>

<style>
</style>