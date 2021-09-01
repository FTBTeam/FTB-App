<template>
  <div class="flex flex-col h-full w-full p-2">
    <div class="flex flex-row items-center">
      <h1 class="text-2xl max-w-xs overflow-hidden" style="text-overflow: ellipsis;">
        {{ friend.profile.display }}
      </h1>
      <div class="ml-auto mr-2 mt-6">
        <font-awesome-icon
          icon="ban"
          class="mx-2 cursor-pointer text-muted hover:text-white"
          title="Block"
          @click="blockFriend"
        />
        <font-awesome-icon
          icon="trash"
          class="mx-2 cursor-pointer text-muted hover:text-white"
          title="Remove"
          @click="removeFriend"
        />
        <font-awesome-icon
          icon="globe"
          class="mx-2 cursor-pointer text-muted hover:text-white"
          v-if="friend.currentServer"
          @click="openServer"
        />
      </div>
    </div>
    <div class="chat-area flex-1 overflow-y-scroll overflow-x-hidden mb-2" ref="chat" v-on:scroll="onScroll">
      <div class="flex flex-row" v-for="m in sortedMessages" :key="JSON.stringify(m)">
        <div :class="`${m.author === shortHash ? 'ml-auto mr-2 text-right' : 'ml-2 text-left'} my-2 flex flex-col`">
          <p class=" p-3 bg-input rounded text-left" @click="onMessageClick" v-html="linkify(m.content)"></p>
          <small :class="`${m.author === shortHash ? 'ml-auto text-right ' : ''} text-muted text-xs`">{{
            (m.date / 1000) | momentFromNow
          }}</small>
        </div>
      </div>
    </div>
    <div class="flex flex-col items-stretch">
      <small v-if="!friend.online" class="text-xs text-center">{{ friend.profile.display }} is currently offline</small>
      <div class="w-full flex flex-row items-center relative">
        <input
          :class="
            `bg-background focus:outline-none border block flex-1 text-2xl transition-all duration-200 ease-in-out p-2 rounded appearance-none leading-normal text-gray-300 border-background-lighten shadow`
          "
          v-model="newMessage"
          @keydown.enter="send"
          :disabled="!friend.online"
        />
        <div class="bg-background-lighten p-4 h-full">
          <font-awesome-icon icon="paper-plane" />
        </div>
      </div>
    </div>
    <FTBModal v-if="checkLink !== null" :visible="checkLink !== null" @dismiss-modal="hideModal">
      <ConfirmationModal
        title="Are you sure you want to open this link?"
        :message="
          `You're about to go to ${checkLink}, we do not control contents of this link and cannot be held responsible for anything caused by it. Please ensure you trust this link before continuing. Are you sure you wish to continue?`
        "
        :yesAction="navigateToLink"
        :noAction="hideModal"
      />
    </FTBModal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Friend } from '../../modules/auth/types';
import { Message } from '../../types';
import ConfirmationModal from '../modals/ConfirmationModal.vue';
import FTBModal from '../FTBModal.vue';
import platform from '@/utils/interface/electron-overwolf';

@Component({
  components: {
    ConfirmationModal,
    FTBModal,
  },
})
export default class FriendChat extends Vue {
  @Prop() private friend!: Friend;
  @Prop() private messages!: Message[];
  @Prop() private sendMessage!: (message: string) => void;

  @Prop() shortHash!: string;
  @Prop() removeFriend!: () => Promise<boolean | string>;
  @Prop() blockFriend!: () => Promise<boolean | string>;

  private newMessage = '';
  private whitelistedDomains: string[] = [
    'creeperhost.net',
    'feed-the-beast.com',
    'minetogether.io',
    'creeper.host',
    'playat.ch',
    'ch.tools',
  ];

  private checkLink: string | null = null;

  private lastScroll = 0;
  private hasScrolled = false;

  public hideModal() {
    this.checkLink = null;
  }

  public linkify(text: string): string {
    const urlRegex = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gi;
    return text.replace(urlRegex, url => {
      return '<a href="' + url + '" target="_blank">' + url + '</a>';
    });
  }

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
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }

  public onMessageClick(event: any) {
    if (event.target != null) {
      if (event.target.tagName === 'A') {
        const url = event.target.href;
        const domain = url.match(/^(?:https?:\/\/)?(?:[^@\/\n]+@)?(?:www\.)?([^:\/?\n]+)/)[0];
        const whitelistedDomain = this.whitelistedDomains.find(d => domain.indexOf(d) !== -1);
        if (whitelistedDomain === undefined) {
          event.preventDefault();
          this.checkLink = event.target.href;
        }
      }
    }
  }

  public onScroll() {
    const chat: Element = this.$refs.chat as Element;
    const pos = chat.scrollTop;
    const maximum = chat.scrollHeight - chat.clientHeight;
    if (pos - this.lastScroll < -1) {
      this.hasScrolled = true;
    } else if (pos >= maximum) {
      this.hasScrolled = false;
    }
    this.lastScroll = pos;
  }

  public beforeUpdate() {
    const chat: Element = this.$refs.chat as Element;
    const pos = chat.scrollTop;
    const maximum = chat.scrollHeight - chat.clientHeight;
    if (!this.hasScrolled) {
      setTimeout(() => {
        this.scrollToLastChild();
      }, 10);
    }
  }

  public mounted() {
    this.scrollToLastChild();
  }

  public send() {
    this.sendMessage(this.newMessage);
    this.newMessage = '';
  }

  public navigateToLink() {
    platform.get.utils.openUrl(this.checkLink as string);
    this.checkLink = null;
  }

  public openServer() {
    platform.get.utils.openUrl(`ftb://server/${this.friend.currentServer}`);
  }
}
</script>

<style>
a {
  cursor: pointer !important;
}
</style>
