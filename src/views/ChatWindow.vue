<template>
  <div id="app" class="theme-dark">
    <title-bar />
    <div class="flex flex-row h-full">
        <friends-list :showPage="showPage" :hidePage="hidePage" :currentPage="currentPage" :messages="messages"  :friends="friends" :activeFriend="friend !== null ? friend.shortHash : undefined"></friends-list>
        <div class="bg-navbar flex-1">
            <AddFriend v-if="currentPage === 'addFriend'"></AddFriend>
            <FriendChat v-if="currentPage === 'chatFriend' && friend !== null" :key="friend.shortHash" :friend="friend" :removeFriend="removeFriend" :shortHash="auth.token.mc.mtusername" :messages="messages[friend.shortHash]" :sendMessage="sendMessage"></FriendChat>
        </div>
    </div>
  </div>
</template>

<script lang="ts">
import TitleBar from '@/components/TitleBar.vue';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { ipcRenderer } from 'electron';
import FriendsList from '@/components/chat/FriendsList.vue';
import AddFriend from '@/components/chat/AddFriend.vue';
import FriendChat from '@/components/chat/FriendChat.vue';
import { Friend, AuthState } from '../modules/auth/types';
import {State, Action} from 'vuex-class';
import { Messages, UnreadMessages, Message, FriendListResponse } from '../types';

@Component({
  components: {
    TitleBar,
    'friends-list': FriendsList,
    AddFriend,
    FriendChat,
  },
})
export default class ChatWindow extends Vue {
    @State('auth')
    private auth!: AuthState;
    private currentPage: string = '';
    private friend: Friend | null = null;
    private messages: Messages = {};
    private friends: FriendListResponse = {friends: [], requests: []};

    @Action('removeFriend', {namespace: "auth"})
    private removeFriendAction!: (hash: string) => Promise<boolean | string>

    public expand() {
        ipcRenderer.send('expandMeScotty', { width: 800 });
    }

    public retract() {
        ipcRenderer.send('expandMeScotty', { width: 300 });
    }

    public showPage(page: string, friend?: Friend) {
        if (this.currentPage === page) {
            if (this.friend && friend && this.friend.hash === friend.hash) {
                this.hidePage();
                return;
            }
        }
        this.expand();
        if (friend) {
            this.friend = friend;
            if (this.messages[friend.shortHash]) {
                let messages = this.messages[friend.shortHash];
                messages = messages.map((message) => {
                    message.read = true;
                    return message;
                });
                Vue.set(this.messages, this.friend.shortHash, messages);
            }
            this.$forceUpdate();
        }
        setTimeout(() => {
            this.currentPage = page;
        }, 100);
    }

    public hidePage() {
        this.retract();
        this.friend = null;
        this.currentPage = '';
    }

    public async removeFriend(){
        if(this.friend === null || this.friend.hash === undefined){
            return;
        }
        let success = await this.removeFriendAction(this.friend.hash);
        if(typeof success === "string"){

        } else {
            if(success){
                this.hidePage();
                ipcRenderer.send('getFriends');
                ipcRenderer.send('checkFriends');
            }
        }
    }

    public sendMessage(message: string) {
        if (this.friend === null || this.auth.token === null) {
            return;
        }
        ipcRenderer.send('sendMessage', {friend: this.friend, message});
        let messages: Message[];
        if (this.messages[this.friend.shortHash] === undefined) {
            messages = [];
        } else {
            messages = this.messages[this.friend.shortHash];
        }
        messages.push({content: message, date: new Date().getTime(), author: this.auth.token.mc.mtusername, read: true});
        Vue.set(this.messages, this.friend.shortHash, messages);
    }

    public mounted() {
        this.retract();
        ipcRenderer.on('newMessage', (event, data) => {
            let messages: Message[];
            if (this.messages[data.from] === undefined) {
                messages = [];
            } else {
                messages = this.messages[data.from];
            }
            messages.push({content: data.message, author: data.from, date: data.date, read: this.currentPage === 'chatFriend' && this.friend?.shortHash === data.from});
            Vue.set(this.messages, data.from, messages);
        });
        ipcRenderer.on('newFriendRequest', (event, data) => {
            let requests = this.friends.requests;
            requests.push({shortHash: data.from, name: data.displayName, friendCode: data.friendCode, accepted: false})
            Vue.set(this.friends, 'requests', requests);
        });
        ipcRenderer.send('checkFriends');
        setInterval(() => {
            ipcRenderer.send('checkFriends');
        }, 30 * 1000);
        ipcRenderer.send('getFriends');
        ipcRenderer.on('ooohFriend', (_, data) => {
            this.friends = data;
        });
    }
}
</script>

<style lang="scss">
#app {
  margin: 0;
  font-family: "Raleway", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--color-text);
  height: 100vh;
  display: flex;
  flex-direction: column;
  .container {
    height: 100%;
    display: flex;
    flex-direction: row;
    background-color: var(--color-background);
  }
}
</style>
