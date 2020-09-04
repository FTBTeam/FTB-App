<template>
  <div id="app" class="theme-dark">
    <title-bar />
    <div class="flex flex-row h-full" v-if="isMinecraftLinked">
        <friends-list :showPage="showPage" :hidePage="hidePage" :currentPage="currentPage" :messages="messages"  :friends="friends" :activeFriend="friend !== null ? friend.shortHash : undefined"></friends-list>
        <div class="bg-navbar flex-1">
            <AddFriend v-if="currentPage === 'addFriend'"></AddFriend>
            <FriendChat v-if="currentPage === 'chatFriend' && friend !== null" :key="friend.shortHash" :friend="friend" :removeFriend="removeFriend" :blockFriend="blockFriend" :shortHash="auth.token.mc.chat.hash.short" :messages="messages[friend.shortHash]" :sendMessage="sendMessage"></FriendChat>
        </div>
    </div>
    <div class="flex flex-col h-full justify-center"  v-else>
        <font-awesome-icon icon="user" class="mx-auto" style="font-size: 25vw;"></font-awesome-icon>
        <h2 class="text-center">In order to use this feature, you must complete your account setup</h2>
        <ftb-button color="primary" class="mx-auto py-2 px-4 my-2" @click="openProfile">Click here to continue</ftb-button>
    </div>
  </div>
</template>

<script lang="ts">
import TitleBar from '@/components/TitleBar.vue';
import FTBButton from '@/components/FTBButton.vue';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { ipcRenderer } from 'electron';
import FriendsList from '@/components/chat/FriendsList.vue';
import AddFriend from '@/components/chat/AddFriend.vue';
import FriendChat from '@/components/chat/FriendChat.vue';
import { Friend, AuthState } from '../modules/auth/types';
import {State, Action} from 'vuex-class';
import { Messages, UnreadMessages, Message, FriendListResponse } from '../types';
import { SettingsState, Settings } from '../modules/settings/types';

@Component({
  components: {
    TitleBar,
    'friends-list': FriendsList,
    AddFriend,
    FriendChat,
    'ftb-button': FTBButton,
  },
})
export default class ChatWindow extends Vue {
    @State('auth')
    private auth!: AuthState;
    @State('settings')
    private settings!: SettingsState;
    private currentPage: string = '';
    private friend: Friend | null = null;
    private messages: Messages = {};
    private friends: FriendListResponse = {friends: [], requests: []};

    @Action('removeFriend', {namespace: 'auth'})
    private removeFriendAction!: (hash: string) => Promise<boolean | string>;

    @Action('saveSettings', {namespace: 'settings'})
    private saveSettings!: (settings: Settings) => Promise<boolean | string>;

    get isMinecraftLinked(){
        let p = this.auth.token?.accounts.find((s) => s.identityProvider === "mcauth");
        return p !== undefined && p != null;
    }

    public openProfile(){
        ipcRenderer.send('openLink', "https://minetogether.io/profile/connections");
    }

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

    public async blockFriend(){
         if (this.friend === null || this.friend.hash === undefined) {
            return;
        }
        const success = await this.removeFriendAction(this.friend.hash);
        if (typeof success === 'string') {

        } else {
            if (success) {
                ipcRenderer.send('blockFriend', this.friend.hash)
                this.hidePage();
                ipcRenderer.send('checkFriends');
            }
        }
    }

    public async removeFriend() {
        if (this.friend === null || this.friend.hash === undefined) {
            return;
        }
        const success = await this.removeFriendAction(this.friend.hash);
        if (typeof success === 'string') {

        } else {
            if (success) {
                this.hidePage();
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
        messages.push({content: message, date: new Date().getTime(), author: this.auth.token.mc.chat.hash.short, read: true});
        Vue.set(this.messages, this.friend.shortHash, messages);
    }

    public mounted() {
        this.retract();
        ipcRenderer.on('updateSettings', (event, data) => {
            this.settings.settings = data;
        });
        ipcRenderer.on('newMessage', (event, data) => {
            if(this.settings.settings.blockedUsers.indexOf(data.from) !== -1){
                return;
            }
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
            const requests = this.friends.requests;
            const existing = requests.find((r) => r.shortHash === data.from);
            if (existing !== undefined) {
                return;
            }
            requests.push({shortHash: data.from, name: data.displayName, friendCode: data.friendCode, accepted: false});
            Vue.set(this.friends, 'requests', requests);
        });
        console.log("Checking if Minecraft is linked")
        if(this.isMinecraftLinked){
            console.log("Checking friends")
            ipcRenderer.send('checkFriends');
            setInterval(() => {
                ipcRenderer.send('checkFriends');
            }, 30 * 1000);
            console.log("Getting friends")
            ipcRenderer.send('getFriends');
            ipcRenderer.on('ooohFriend', (_, data) => {
                console.log("Received friends")
                Vue.set(this.friends, 'friends',  data.friends);
                let requests = this.friends.requests;
                requests = data.requests.map((request: Friend) => {
                    const existing = this.friends.requests.find((f) => f.shortHash === request.shortHash);
                    if (existing !== undefined) {
                        request.friendCode = existing.friendCode;
                        request.name = existing.name;
                    }
                    return request;
                });
                Vue.set(this.friends, 'requests', requests);
            });
        }
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
