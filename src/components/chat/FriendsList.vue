<template>
  <div class="flex flex-col h-full bg-background" style="width: 300px;">
      <div class="profile bg-navbar flex flex-row items-center relative" style="height: 75px;">
        <div class="minimtg pointer-events-none" ></div>
        <div class="p-4 flex flex-row items-center">
        <img :src="`https://minotar.net/helm/e1949d31-4f97-423a-8229-690fd6756b1c`" style="margin-right: 0.75em;" width="30px" class="rounded-full" />
        <div class="flex flex-col">
            <p class="text-lg" v-if="auth.token !== null">{{auth.token.username}}</p>
            <p class="text-xs cursor-pointer" v-if="auth.token !== null" key="friendCode" @click="copyFriendCode">{{auth.token.mc.friendCode}} <transition name="fade"><span key="copied" v-if="copied"> - Copied!</span></transition></p>
        </div>
        </div>
      </div>
      <div class="friends flex flex-col overflow-y-auto" style="max-height: 300px;"> 
        <div class="bg-background-lighten p-2 flex flex-row">
            <p class="ml-2">Friends</p>
            <input type="search" class="bg-input text-xs mx-2" style="padding: 0 2px;" placeholder="Search..." v-if="showSearch" v-model="search"/>
            <div class="icons ml-auto">
                <font-awesome-icon icon="search" class="mx-2 cursor-pointer" @click="toggleSearch"/>
                <font-awesome-icon icon="user-plus" @click="openAddFriendUI" class="mx-2 cursor-pointer" />
            </div>
        </div>
        <div :class="`flex flex-row p-2 items-center ${currentPage === 'chatFriend' && activeFriend !== undefined && activeFriend === friend.shortHash ? 'bg-background-lighten' : 'hover:bg-background-lighten'} cursor-pointer`" v-for="friend in currentFriends" :key="friend.id" @click="openFriendChat(friend)">
            <p :class="`ml-2 cursor-pointer ${friend.online ? 'text-white font-bold' : 'text-muted font-normal'}`">{{friend.name}} </p>
            <p v-if="unreadMessages[friend.shortHash] > 0" class="bg-red-600 rounded-full px-2 ml-2 text-xs">{{unreadMessages[friend.shortHash]}}</p>
            <div class="icons ml-auto">
                <font-awesome-icon v-if="friend.currentPack && friend.currentPack.length > 0" icon="gamepad" class="mx-2 cursor-pointer" :title="friend.currentPack"/>
                <font-awesome-icon v-if="friend.online" icon="comments" class="mx-2 cursor-pointer" />
            </div>
        </div>
        <div v-if="currentFriends.length === 0 && search.length > 0" class="flex flex-col items-center mt-4 text-sm">
            <font-awesome-icon icon="sad-tear" size="lg"/>
            <p class="p-2 text-center">No results</p>
        </div>
        <div v-else-if="currentFriends.length === 0 && search.length === 0" class="flex flex-col items-center mt-4 text-sm">
            <font-awesome-icon icon="sad-tear" size="lg"/>
            <p class="p-2 text-center">It looks like you don't have any friends added at the moment!</p>
        </div>
      </div>
      <div class="friends flex flex-col overflow-y-auto">
        <div class="bg-background-lighten p-2 flex flex-row mt-10 cursor-pointer"  @click="toggleRequests">
            <p class="ml-2 cursor-pointer">Pending Requests</p>
            <p v-if="friendRequests.length > 0 && !showRequests" class="bg-red-600 rounded-full px-2 ml-2 text-xs">{{friendRequests.length}}</p>
            <div class="icons ml-auto">
                <font-awesome-icon :icon="!showRequests ? 'chevron-left' : 'chevron-down'" class="mx-2 cursor-pointer" />
            </div>
        </div>
        <div v-if="showRequests">
            <div class="flex flex-row p-2 items-center hover:bg-background-lighten cursor-pointer" v-for="friend in friendRequests" :key="friend.id">
                <p class="ml-2">{{friend.name}}</p>
                <div class="icons ml-auto" v-if="friend.online && friend.friendCode">
                    <font-awesome-icon icon="times" class="mx-2 cursor-pointer hover:text-red-500" />
                    <font-awesome-icon icon="check" class="mx-2 cursor-pointer hover:text-green-500" @click="acceptFriendRequest(friend.hash, friend.friendCode, friend.name)" />
                </div>
            </div>
        </div>
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import {shell, ipcRenderer, clipboard} from 'electron';
import {State, Action} from 'vuex-class';
import store from '@/store';
import config from '@/config';
import { AuthState, Friend } from '../../modules/auth/types';
import { Messages, FriendListResponse } from '../../types';
import { shortenHash } from '../../utils';

interface UnreadMessages {
    [index: string]: number;
}

@Component({
    props: [
        'showPage',
        'hidePage',
        'currentPage',
        'messages',
        'friends',
        'activeFriend'
    ],
})
export default class MainChat extends Vue {
    @State('auth')
    private auth!: AuthState;
    @Action('getFriendCode', {namespace: 'auth'})
    private getFriendCode!: () => void;

    private copied: boolean = false;
    private showSearch: boolean = false;
    private showRequests: boolean = true;
    private search: string = '';

    @Prop()
    private friends!: FriendListResponse;
    @Prop()
    private currentPage!: string;
    @Prop()
    private messages!: Messages;
    @Prop()
    private hidePage!: () => void;
    @Prop()
    private showPage!: (page: string, friend?: Friend) => void;

    public toggleSearch() {
        this.showSearch = !this.showSearch;
    }
    public toggleRequests() {
        this.showRequests = !this.showRequests;
    }

    @Watch('auth', {deep: true})
    public async onAuthChange(newVal: AuthState, oldVal: AuthState) {
      if ((newVal.token !== null && newVal.token.mc.friendCode === undefined) || (JSON.stringify(newVal.token) !== JSON.stringify(oldVal.token))) {
        this.getFriendCode();
      }
    }

    public openFriendChat(friend: Friend) {
        this.showPage('chatFriend', friend);
        this.$forceUpdate();
    }

    public openAddFriendUI() {
        if (this.currentPage === 'addFriend') {
            this.hidePage();
        } else {
            this.showPage('addFriend');
        }
    }

    public acceptFriendRequest(hash: string, friendCode: string, name: string){
        ipcRenderer.send('acceptFriendRequest', {hash, target: shortenHash(hash), friendCode, name, ourName: this.auth.token?.username});
    }

    get unreadMessages() {
        const unread: UnreadMessages = {};
        Object.keys(this.messages).forEach((shortHash) => {
            unread[shortHash] = this.messages[shortHash].filter((m) => !m.read).length;
        });
        return unread;
    }


    get currentFriends() {
        // return [{name: "Gaz492"}, {name: "Paul_T"}, {name: "Jake_Evans"}];
        let friends = this.friends.friends.filter((a) => a.accepted).sort((a, b) => Number(b.online) - Number(a.online));
        if (this.search.length > 0) {
            friends = friends.filter((a) => a.name.indexOf(this.search) !== -1);
        }
        return friends;
    }

    get friendRequests() {
        return this.friends.requests;
    }

    public mounted() {
        this.showRequests = this.friendRequests.length > 0;
    }

    public copyFriendCode() {
        if (this.auth.token) {
            clipboard.writeText(this.auth.token.mc.friendCode);
            this.copied = true;
            setTimeout(() => this.copied = false, 2000);
        }
    }
}
</script>

<style>
  .minimtg{
    background-image: url("../../assets/mtg-tiny-desat.png");
    width: 300px;
    height: 100%;
    position: absolute;
    text-align: center;
    filter: brightness(0.7);
    -webkit-mask-image: -webkit-gradient(linear, left top, left bottom, from(rgba(0,0,0,0.4)), to(rgba(0,0,0,0)));
  }
</style>