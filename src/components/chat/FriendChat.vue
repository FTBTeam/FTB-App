<template>
  <div class="flex flex-col h-full w-full p-2">
      <div class="flex flex-row items-center">
        <h1 class="text-2xl max-w-xs overflow-hidden" style="text-overflow: ellipsis;">{{friend.name}}</h1>
        <div class="ml-auto mr-2 mt-6">
            <font-awesome-icon icon="ban" class="mx-2 cursor-pointer text-muted hover:text-white" title="Block"  @click="blockFriend"/>
            <font-awesome-icon icon="trash" class="mx-2 cursor-pointer text-muted hover:text-white" title="Remove"  @click="removeFriend" />
            <font-awesome-icon icon="globe" class="mx-2 cursor-pointer text-muted hover:text-white"  v-if="friend.currentServer"  @click="openServer" />
        </div>
      </div>
      <div class="chat-area flex-1 overflow-y-scroll overflow-x-hidden mb-2" ref="chat">
        <div class="flex flex-row" v-for="m in sortedMessages" :key="JSON.stringify(m)">
            <div :class="`${m.author === shortHash ? 'ml-auto mr-2 text-right' : 'ml-2 text-left'} my-2 flex flex-col`">
                <p class=" p-3 bg-input rounded text-left">{{m.content}}</p>
                <small :class="`${m.author === shortHash ? 'ml-auto text-right ' : ''} text-muted text-xs`">{{m.date / 1000 | momentFromNow}}</small>
            </div>
        </div>
      </div>
      <div class="flex flex-col items-stretch">
          <small v-if="!friend.online" class="text-xs text-center">{{friend.name}} is currently offline</small>
        <div class="w-full flex flex-row items-center relative">
        <input :class="`bg-background focus:outline-none border block flex-1 text-2xl transition-all duration-200 ease-in-out p-2 rounded appearance-none leading-normal text-gray-300 border-background-lighten shadow`" v-model="newMessage" @keydown.enter="send" :disabled="!friend.online" />
            <div class="bg-background-lighten p-4 h-full">
                <font-awesome-icon icon="paper-plane" />
            </div>
        </div>
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { shell, ipcRenderer, clipboard } from 'electron';
import { State, Action } from 'vuex-class';
import store from '@/store';
import config from '@/config';
import { AuthState, Friend } from '../../modules/auth/types';
import FTBInput from '../FTBInput.vue';
import FTBButton from '../FTBButton.vue';
import { Message } from '../../types';



@Component({
    components: {
        'ftb-input': FTBInput,
        'ftb-button': FTBButton,
    },
    props: [
        'friend',
        'shortHash',
        'messages',
        'sendMessage',
        'removeFriend',
        'blockFriend'
    ],
})
export default class FriendChat extends Vue {
    @Prop()
    private friend!: Friend;
    @Prop()
    private shortHash!: string;

    private newMessage: string = '';
    @Prop()
    private removeFriend!: () => Promise<boolean | string>;
    @Prop()
    private blockFriend!: () => Promise<boolean | string>;

    @Prop()
    private messages!: Message[];
    @Prop()
    private sendMessage!: (message: string) => void;

    get sortedMessages() {
        if (!this.messages) {
            return [];
        }
        return this.messages.sort((a: Message, b: Message) => a.date - b.date);
    }

    public scrollToLastChild() {
        const chat: Element = this.$refs.chat as Element;
        const el: Element = chat.lastChild as Element;
        if (el !== null) {
            el.scrollIntoView({behavior: 'smooth'});
        }
    }

    public beforeUpdate() {
        const chat: Element = this.$refs.chat as Element;
        const pos = chat.scrollTop;
        const maximum = chat.scrollHeight - chat.clientHeight;
        if (pos >= maximum) {
            setTimeout(() => {
               this.scrollToLastChild();
            }, 20);
        }
    }

    public mounted() {
        this.scrollToLastChild();
    }

    public send() {
        this.sendMessage(this.newMessage);
        this.newMessage = '';
    }

    public openServer(){
        ipcRenderer.send('openLink', `ftb://server/${this.friend.currentServer}`)
    }
}
</script>

<style>
</style>