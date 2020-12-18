<template>
  <div id="app" class="theme-dark">
    <title-bar />
    <div class="flex flex-col h-full justify-center" v-if="!websocket.socket.isConnected">
        <font-awesome-icon icon="sync-alt" spin class="mx-auto" style="font-size: 25vw;"></font-awesome-icon>
    </div>
    <div class="flex flex-row h-full" v-else-if="isMinecraftLinked">
        <friends-list :showPage="showPage" :hidePage="hidePage" :currentPage="currentPage" :messages="messages"  :friends="friends" :activeFriend="friend !== null ? friend.profile.chat.hash.medium : undefined" :getFriends="getFriends" :loading="loadingFriends"></friends-list>
        <div class="bg-navbar flex-1">
            <AddFriend v-if="currentPage === 'addFriend'"></AddFriend>
            <FriendChat v-if="currentPage === 'chatFriend' && friend !== null" :key="friend.profile.chat.hash.medium" :friend="friend" :removeFriend="removeFriend" :blockFriend="blockFriend" :shortHash="friend.profile.chat.hash.medium" :messages="messages[friend.profile.chat.hash.medium]" :sendMessage="sendMessage"></FriendChat>
        </div>
    </div>
    <div class="flex flex-col h-full justify-center" v-else-if="auth.loggingIn">
        <font-awesome-icon icon="sync-alt" spin class="mx-auto" style="font-size: 25vw;"></font-awesome-icon>
    </div>
    <div class="flex flex-col h-full justify-center" v-else>
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
import { SocketState } from '../modules/websocket/types';

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
    @State('websocket')
    private websocket!: SocketState;
    @Action('sendMessage') public sendIRCMessage: any;

    private hasLoaded = false;
    private loadingFriends: boolean = true;
    private currentPage = '';
    private friend: Friend | null = null;
    private messages: Messages = {};
    private friends: FriendListResponse = {friends: [], requests: []};

    @Action('removeFriend', {namespace: 'auth'})
    private removeFriendAction!: (hash: string) => Promise<boolean | string>;

    @Action('saveSettings', {namespace: 'settings'})
    private saveSettings!: (settings: Settings) => Promise<boolean | string>;

    @Action('registerIRCCallback')
    private registerIRCCallback: any;

    @Watch('auth', {deep: true})
    public async onAuthChange(newVal: AuthState, oldVal: AuthState) {
        if(newVal.token?.accounts.find((s: any) => s.identityProvider === "mcauth") !== undefined && !this.hasLoaded && this.websocket.socket.isConnected){
            this.hasLoaded = true;
            if(newVal.token){
                this.getFriends(newVal.token.mc.hash.long);
            }
            setInterval(() => {
                if(newVal.token){
                    this.getFriends(newVal.token.mc.hash.long);
                }
            }, 30 * 1000);
        }
    }

    @Watch('websocket', {deep: true})
    public async onSocketChange(newVal: SocketState, oldVal: SocketState) {
        if(newVal.socket.isConnected){
            if(this.auth.token?.accounts.find((s: any) => s.identityProvider === "mcauth") !== undefined && !this.hasLoaded) {
                this.hasLoaded = true;
                if(this.auth.token){
                    this.getFriends(this.auth.token.mc.hash.long);
                }
                setInterval(() => {
                    if(this.auth.token){
                        this.getFriends(this.auth.token.mc.hash.long);
                    }
                }, 30 * 1000);
            }
        }
    }

    public getFriends(longHash?: string){
        this.loadingFriends = true;
        if(longHash === undefined){
            longHash = this.auth.token?.mc.hash.long;
        }
        if(Array.isArray(longHash)){
            longHash = longHash[0];
        }
        let requestPending = true;
        let shouldCancel = false;
        let timeout = setTimeout(() => {
            this.loadingFriends = false;
        }, 10 * 1000);
        this.sendIRCMessage({payload: {type: 'getFriends', hash: longHash}, callback: (data: any) => {
            data = data.friends;
            Vue.set(this.friends, 'friends',  data.friends);
            let requests = this.friends.requests;
            requests = data.requests.map((request: Friend) => {
                const existing = this.friends.requests.find((f) => f.profile.hash === request.profile.hash);
                if (existing !== undefined) {
                    request.profile.friendCode = existing.profile.friendCode;
                    request.profile.display = existing.profile.display;
                }
                return request;
            });
            Vue.set(this.friends, 'requests', requests);
            this.loadingFriends = false;
            clearInterval(timeout)
        }});
    }

    get isMinecraftLinked(){
        const p = this.auth.token?.accounts.find((s) => s.identityProvider === "mcauth");
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
            if (this.messages[friend.profile.chat.hash.medium]) {
                let messages = this.messages[friend.profile.chat.hash.medium];
                messages = messages.map((message) => {
                    message.read = true;
                    return message;
                });
                Vue.set(this.messages, this.friend.profile.chat.hash.medium, messages);
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
                this.sendIRCMessage({payload: {type: "blockFriend", hash: this.friend.hash}})
                this.hidePage();    
               if(this.auth.token){
                    this.getFriends(this.auth.token.mc.hash.long);
                }
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
                if(this.auth.token){
                    this.getFriends(this.auth.token.mc.hash.long);
                }
            }
        }
    }

    public sendMessage(message: string) {
        if(message.length < 1){
            return;
        }
        if (this.friend === null || this.auth.token === null) {
            return;
        }
        this.sendIRCMessage({payload: {type: 'ircSendMessage', nick: this.friend.profile.chat.hash.medium, message}});
        let messages: Message[];
        if (this.messages[this.friend.profile.chat.hash.medium] === undefined) {
            messages = [];
        } else {
            messages = this.messages[this.friend.profile.chat.hash.medium];
        }
        messages.push({content: message, date: new Date().getTime(), author: this.auth.token.mc.chat.hash.short, read: true});
        Vue.set(this.messages, this.friend.profile.chat.hash.medium, messages);
    }

    public mounted() {
        this.retract();
        // ipcRenderer.on('updateSettings', (event, data) => {
        //     this.settings.settings = data;
        // });
        this.registerIRCCallback((data: any) => {
            if(data.jsEvent === "message"){
                if(data.ircType === "privmsg"){
                    let from = data.nick;
                    let message = data.message;
                    if(this.settings.settings.blockedUsers.indexOf(from) !== -1){
                        return;
                    }
                    let messages: Message[];
                    if (this.messages[from] === undefined) {
                        messages = [];
                    } else {
                        messages = this.messages[from];
                    }
                    messages.push({content: message, author: from, date: new Date().getTime(), read: this.currentPage === 'chatFriend' && this.friend?.profile.chat.hash.medium === from});
                    Vue.set(this.messages, from, messages);
                }
            } else if(data.jsEvent === "ctcp"){
                let request = data.data;
                if(data.type === "newFriend"){
                    const requests = this.friends.requests;
                    const existing = requests.find((r) => r.profile.chat.hash.medium === data.nick);
                    if (existing !== undefined) {
                        return;
                    }
                    let profile = data.profile;
                    requests.push({profile, name: profile.display, accepted: false});
                    Vue.set(this.friends, 'requests', requests);
                }
                if(this.auth.token){
                    this.getFriends(this.auth.token.mc.hash.long);
                }
            }
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
