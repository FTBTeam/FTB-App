<!--<template>-->
<!--  <div id="app" class="theme-dark">-->
<!--    <title-bar />-->
<!--    <div class="flex flex-col h-full justify-center" v-if="!websocket.socket.isConnected">-->
<!--      <font-awesome-icon icon="sync-alt" spin class="mx-auto" style="font-size: 25vw"></font-awesome-icon>-->
<!--    </div>-->
<!--    <div class="flex flex-row h-full" v-else-if="isMinecraftLinked">-->
<!--      <friends-list-->
<!--        :showPage="showPage"-->
<!--        :hidePage="hidePage"-->
<!--        :currentPage="currentPage"-->
<!--        :messages="messages"-->
<!--        :friends="friends"-->
<!--        :activeFriend="friend !== null ? friend.mediumHash : undefined"-->
<!--        :getFriends="getFriends"-->
<!--        :loading="loadingFriends"-->
<!--      ></friends-list>-->
<!--      -->
<!--      <div class="bg-navbar flex-1">-->
<!--        <AddFriend v-if="currentPage === 'addFriend'"></AddFriend>-->
<!--        <FriendChat-->
<!--          v-if="currentPage === 'chatFriend' && friend !== null"-->
<!--          :key="friend.mediumHash"-->
<!--          :friend="friend"-->
<!--          :removeFriend="removeFriend"-->
<!--          :blockFriend="blockFriend"-->
<!--          :shortHash="friend.mediumHash"-->
<!--          :messages="messages[friend.mediumHash]"-->
<!--          :sendMessage="sendMessage"-->
<!--        ></FriendChat>-->
<!--      </div>-->
<!--    </div>-->
<!--    <div class="flex flex-col h-full justify-center" v-else-if="auth.loggingIn">-->
<!--      <font-awesome-icon icon="sync-alt" spin class="mx-auto" style="font-size: 25vw"></font-awesome-icon>-->
<!--    </div>-->
<!--    <div class="flex flex-col h-full justify-center" v-else>-->
<!--      <font-awesome-icon icon="user" class="mx-auto" style="font-size: 25vw"></font-awesome-icon>-->
<!--      <h2 class="text-center">In order to use this feature, you must complete your account setup</h2>-->
<!--      <ftb-button color="primary" class="mx-auto py-2 px-4 my-2" @click="openProfile"-->
<!--        >Click here to continue</ftb-button-->
<!--      >-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->

<!--<script lang="ts">-->
<!--import TitleBar from '@/components/layout/TitleBar.vue';-->
<!--import { Component, Vue, Watch } from 'vue-property-decorator';-->
<!--import FriendsList from '@/components/templates/chat/FriendsList.vue';-->
<!--import AddFriend from '@/components/templates/chat/AddFriend.vue';-->
<!--import FriendChat from '@/components/templates/chat/FriendChat.vue';-->
<!--import { AuthState, Friend } from '../modules/auth/types';-->
<!--import { Action, State } from 'vuex-class';-->
<!--import { FriendListResponse, Message, Messages } from '../types';-->
<!--import { Settings, SettingsState } from '../modules/settings/types';-->
<!--import { SocketState } from '../modules/websocket/types';-->
<!--import platform from '@/utils/interface/electron-overwolf';-->

<!--@Component({-->
<!--  components: {-->
<!--    TitleBar,-->
<!--    'friends-list': FriendsList,-->
<!--    AddFriend,-->
<!--    FriendChat,-->
<!--  },-->
<!--})-->
<!--export default class ChatWindow extends Vue {-->
<!--  @Action('sendMessage') public sendIRCMessage: any;-->
<!--  @Action('removeFriend', { namespace: 'auth' }) private removeFriendAction!: (-->
<!--    hash: string,-->
<!--  ) => Promise<boolean | string>;-->
<!--  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: (-->
<!--    settings: Settings,-->
<!--  ) => Promise<boolean | string>;-->
<!--  @Action('registerIRCCallback') private registerIRCCallback: any;-->
<!--  @State('auth') private auth!: AuthState;-->
<!--  @State('settings') private settings!: SettingsState;-->
<!--  @State('websocket') private websocket!: SocketState;-->

<!--  private hasLoaded = false;-->
<!--  private loadingFriends: boolean = false;-->
<!--  private currentPage = '';-->
<!--  private friend: Friend | null = null;-->
<!--  private messages: Messages = {};-->
<!--  private friends: FriendListResponse = { online: [], offline: [], pending: [] };-->

<!--  @Watch('auth', { deep: true })-->
<!--  public async onAuthChange(newVal: AuthState, oldVal: AuthState) {-->
<!--    if (newVal.token?.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined && !this.hasLoaded) {-->
<!--      this.hasLoaded = true;-->
<!--      if (newVal.token) {-->
<!--        this.getFriends(newVal.token.mc.hash.long);-->
<!--      }-->
<!--      setInterval(() => {-->
<!--        if (newVal.token) {-->
<!--          this.getFriends(newVal.token.mc.hash.long);-->
<!--        }-->
<!--      }, 30 * 1000);-->
<!--    }-->
<!--  }-->

<!--  // @Watch('websocket', {deep: true})-->
<!--  // public async onSocketChange(newVal: SocketState, oldVal: SocketState) {-->
<!--  //     if (newVal.socket.isConnected) {-->
<!--  //         if (this.auth.token?.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined && !this.hasLoaded) {-->
<!--  //             this.hasLoaded = true;-->
<!--  //             if (this.auth.token) {-->
<!--  //                 this.getFriends(this.auth.token.mc.hash.long);-->
<!--  //             }-->
<!--  //             setInterval(() => {-->
<!--  //                 if (this.auth.token) {-->
<!--  //                     this.getFriends(this.auth.token.mc.hash.long);-->
<!--  //                 }-->
<!--  //             }, 30 * 1000);-->
<!--  //         }-->
<!--  //     }-->
<!--  // }-->

<!--  get isMinecraftLinked() {-->
<!--    const p = this.auth.token?.accounts.find((s) => s.identityProvider === 'mcauth');-->
<!--    return p !== undefined && p != null;-->
<!--  }-->

<!--  public getFriends(longHash?: string) {-->
<!--    this.loadingFriends = true;-->
<!--    if (longHash === undefined) {-->
<!--      longHash = this.auth.token?.mc.hash.long;-->
<!--    }-->
<!--    if (Array.isArray(longHash)) {-->
<!--      longHash = longHash[0];-->
<!--    }-->
<!--    this.sendIRCMessage({-->
<!--      payload: { type: 'getFriends', hash: longHash },-->
<!--      callback: (data: any) => {-->
<!--        Vue.set(this.friends, 'online', data.online);-->
<!--        Vue.set(this.friends, 'offline', data.offline);-->
<!--        Vue.set(this.friends, 'pending', data.pending);-->
<!--        let requests = this.friends.pending;-->
<!--        requests = data.pending.map((request: Friend) => {-->
<!--          const existing = this.friends.pending.find((f) => f.longHash === request.longHash);-->
<!--          if (existing !== undefined) {-->
<!--            request.friendCode = existing.friendCode;-->
<!--            request.userDisplay = existing.userDisplay;-->
<!--          }-->
<!--          return request;-->
<!--        });-->
<!--        Vue.set(this.friends, 'requests', requests);-->
<!--        this.loadingFriends = false;-->
<!--      },-->
<!--    });-->
<!--  }-->

<!--  public openProfile() {-->
<!--    platform.get.utils.openUrl('https://minetogether.io/profile/connections');-->
<!--  }-->

<!--  public expand() {-->
<!--    platform.get.frame.expandWindow();-->
<!--  }-->

<!--  public retract() {-->
<!--    platform.get.frame.collapseWindow();-->
<!--  }-->

<!--  public showPage(page: string, friend?: Friend) {-->
<!--    if (this.currentPage === page) {-->
<!--      if (this.friend && friend && this.friend.longHash === friend.longHash) {-->
<!--        this.hidePage();-->
<!--        return;-->
<!--      }-->
<!--    }-->
<!--    this.expand();-->
<!--    if (friend) {-->
<!--      this.friend = friend;-->
<!--      if (this.messages[friend.mediumHash]) {-->
<!--        let messages = this.messages[friend.mediumHash];-->
<!--        messages = messages.map((message) => {-->
<!--          message.read = true;-->
<!--          return message;-->
<!--        });-->
<!--        Vue.set(this.messages, this.friend.mediumHash, messages);-->
<!--      }-->
<!--      this.$forceUpdate();-->
<!--    }-->
<!--    setTimeout(() => {-->
<!--      this.currentPage = page;-->
<!--    }, 100);-->
<!--  }-->

<!--  public hidePage() {-->
<!--    this.retract();-->
<!--    this.friend = null;-->
<!--    this.currentPage = '';-->
<!--  }-->

<!--  public async blockFriend() {-->
<!--    if (this.friend === null || this.friend.longHash === undefined) {-->
<!--      return;-->
<!--    }-->
<!--    const success = await this.removeFriendAction(this.friend.longHash);-->
<!--    if (typeof success === 'string') {-->
<!--    } else {-->
<!--      if (success) {-->
<!--        this.sendIRCMessage({ payload: { type: 'blockFriend', hash: this.friend.longHash } });-->
<!--        this.hidePage();-->
<!--        if (this.auth.token) {-->
<!--          this.getFriends(this.auth.token.mc.hash.long);-->
<!--        }-->
<!--      }-->
<!--    }-->
<!--  }-->

<!--  public async removeFriend() {-->
<!--    if (this.friend === null || this.friend.longHash === undefined) {-->
<!--      return;-->
<!--    }-->
<!--    const success = await this.removeFriendAction(this.friend.longHash);-->
<!--    if (typeof success === 'string') {-->
<!--    } else {-->
<!--      if (success) {-->
<!--        this.hidePage();-->
<!--        if (this.auth.token) {-->
<!--          this.getFriends(this.auth.token.mc.hash.long);-->
<!--        }-->
<!--      }-->
<!--    }-->
<!--  }-->

<!--  public sendMessage(message: string) {-->
<!--    if (message.length < 1) {-->
<!--      return;-->
<!--    }-->
<!--    if (this.friend === null || this.auth.token === null) {-->
<!--      return;-->
<!--    }-->
<!--    this.sendIRCMessage({ payload: { type: 'ircSendMessage', nick: this.friend.mediumHash, message } });-->
<!--    let messages: Message[];-->
<!--    if (this.messages[this.friend.mediumHash] === undefined) {-->
<!--      messages = [];-->
<!--    } else {-->
<!--      messages = this.messages[this.friend.mediumHash];-->
<!--    }-->
<!--    messages.push({-->
<!--      content: message,-->
<!--      date: new Date().getTime(),-->
<!--      author: this.auth.token.mc.chat.hash.short,-->
<!--      read: true,-->
<!--    });-->
<!--    Vue.set(this.messages, this.friend.mediumHash, messages);-->
<!--  }-->

<!--  public mounted() {-->
<!--    this.retract();-->
<!--    // ipcRenderer.on('updateSettings', (event, data) => {-->
<!--    //     this.settings.settings = data;-->
<!--    // });-->
<!--    this.registerIRCCallback((data: any) => {-->
<!--      // if (data.jsEvent === 'message') {-->
<!--      //   if (data.ircType === 'privmsg') {-->
<!--      const from = data.nick;-->
<!--      const message = data.message;-->
<!--      if (this.settings.settings.blockedUsers.indexOf(from) !== -1) {-->
<!--        return;-->
<!--      }-->
<!--      let messages: Message[];-->
<!--      if (this.messages[from] === undefined) {-->
<!--        messages = [];-->
<!--      } else {-->
<!--        messages = this.messages[from];-->
<!--      }-->
<!--      messages.push({-->
<!--        content: message,-->
<!--        author: from,-->
<!--        date: new Date().getTime(),-->
<!--        read: this.currentPage === 'chatFriend' && this.friend?.mediumHash === from,-->
<!--      });-->
<!--      Vue.set(this.messages, from, messages);-->
<!--      // }-->
<!--      // } else if (data.jsEvent === 'ctcp') {-->
<!--      //   const request = data.data;-->
<!--      //   if (data.type === 'newFriend') {-->
<!--      //     const requests = this.friends.pending;-->
<!--      //     const existing = requests.find(r => r.mediumHash === data.nick);-->
<!--      //     if (existing !== undefined) {-->
<!--      //       return;-->
<!--      //     }-->
<!--      //     const profile = data.profile;-->
<!--      //     // TODO: (legacy) Figure out what this needs-->
<!--      //     // requests.push({ profile, name: profile.display, accepted: false });-->
<!--      //     Vue.set(this.friends, 'requests', requests);-->
<!--      //   }-->
<!--      //   if (this.auth.token) {-->
<!--      //     this.getFriends(this.auth.token.mc.hash.long);-->
<!--      //   }-->
<!--      // }-->
<!--    });-->
<!--  }-->
<!--}-->
<!--</script>-->

<!--<style lang="scss" scoped>-->
<!--#app {-->
<!--  margin: 0;-->
<!--  font-family: 'Inter';-->
<!--  -webkit-font-smoothing: antialiased;-->
<!--  -moz-osx-font-smoothing: grayscale;-->
<!--  color: var(&#45;&#45;color-text);-->
<!--  height: 100vh;-->
<!--  display: flex;-->
<!--  flex-direction: column;-->
<!--  .container {-->
<!--    height: 100%;-->
<!--    display: flex;-->
<!--    flex-direction: row;-->
<!--    background-color: var(&#45;&#45;color-background);-->
<!--  }-->
<!--}-->
<!--</style>-->
