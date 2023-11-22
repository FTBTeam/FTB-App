<!--<template>-->
<!--  <div class="card-list server-card">-->
<!--    <div class="art">-->
<!--      <img-->
<!--        class="rounded"-->
<!--        width="124"-->
<!--        :src="-->
<!--          server.protoResponse && server.protoResponse.favicon !== undefined && server.protoResponse.favicon.length > 0-->
<!--            ? server.protoResponse.favicon-->
<!--            : art !== undefined && art.length > 0-->
<!--            ? art-->
<!--            : defaultImage-->
<!--        "-->
<!--        alt="placeholder"-->
<!--      />-->
<!--    </div>-->

<!--    <div class="body">-->
<!--      <div class="main">-->
<!--        <div class="font-bold mb-1">{{ server.name }}</div>-->
<!--        <p class="max-2-lines mb-4" v-if="server.protoResponse && server.protoResponse.description !== undefined">-->
<!--          {{-->
<!--            server.protoResponse.description.text-->
<!--              ? server.protoResponse.description.text.replace(/\u00a7[0-9a-fk-or]/gi, '')-->
<!--              : ''-->
<!--          }}-->
<!--        </p>-->

<!--        <div class="buttons">-->
<!--          <FTBButton-->
<!--            @click="goToPage"-->
<!--            :isRounded="false"-->
<!--            color="primary"-->
<!--            class="rounded py-2 px-10 cursor-pointer mr-2"-->
<!--          >-->
<!--            <font-awesome-icon icon="play" size="sm" class="mr-2" /> Join</FTBButton-->
<!--          >-->
<!--          <FTBButton @click="goToPage" :isRounded="false" color="info" class="rounded py-2 px-6 cursor-pointer">-->
<!--            <font-awesome-icon icon="ellipsis-h" size="sm" class="mr-2" /> More-->
<!--          </FTBButton>-->
<!--        </div>-->
<!--      </div>-->
<!--      <aside>-->
<!--        <div class="ml-auto mr-2 opacity-75" v-if="server.protoResponse && server.protoResponse.players">-->
<!--          Players-->
<!--          <span>{{ server.protoResponse.players.online }}/{{ server.protoResponse.players.max }}</span>-->
<!--        </div>-->
<!--      </aside>-->
<!--    </div>-->

<!--    <FTBModal :visible="showMsgBox" @dismiss-modal="hideMsgBox">-->
<!--      <message-modal-->
<!--        :title="msgBox.title"-->
<!--        :content="msgBox.content"-->
<!--        :ok-action="msgBox.okAction"-->
<!--        :cancel-action="msgBox.cancelAction"-->
<!--        :type="msgBox.type"-->
<!--      />-->
<!--    </FTBModal>-->
<!--  </div>-->
<!--</template>-->

<!--<script lang="ts">-->
<!--import { Component, Prop, Vue } from 'vue-property-decorator';-->
<!--import FTBButton from '@/components/atoms/input/FTBButton.vue';-->
<!--// @ts-ignore-->
<!--import placeholderImage from '@/assets/placeholder_art.webp';-->
<!--import MessageModal from '@/components/organisms/modals/MessageModal.vue';-->
<!--import FTBModal from '@/components/atoms/FTBModal.vue';-->
<!--import platform from '@/utils/interface/electron-overwolf';-->

<!--export interface MsgBox {-->
<!--  title: string;-->
<!--  content: string;-->
<!--  type: string;-->
<!--  okAction: () => void;-->
<!--  cancelAction: () => void;-->
<!--}-->

<!--@Component({-->
<!--  components: {-->
<!--    FTBButton,-->
<!--    FTBModal,-->
<!--    'message-modal': MessageModal,-->
<!--  },-->
<!--  props: ['server', 'art'],-->
<!--})-->
<!--export default class ServerCard extends Vue {-->
<!--  private defaultImage: any = placeholderImage;-->
<!--  private showMsgBox = false;-->

<!--  @Prop()-->
<!--  private server: any;-->

<!--  private msgBox: App.MsgBox = {-->
<!--    title: '',-->
<!--    content: '',-->
<!--    type: '',-->
<!--    okAction: Function,-->
<!--    cancelAction: Function,-->
<!--  };-->

<!--  public goToPage() {-->
<!--    platform.get.utils.openUrl(`ftb://server/${this.server.id}`);-->
<!--  }-->

<!--  public comingSoonMsg() {-->
<!--    this.msgBox.type = 'okOnly';-->
<!--    this.msgBox.title = 'Coming Soon';-->
<!--    this.msgBox.okAction = this.hideMsgBox;-->
<!--    this.msgBox.cancelAction = this.hideMsgBox;-->
<!--    this.msgBox.content = `This feature is currently not available, we do however aim to have this feature implemented in the near future`;-->
<!--    this.showMsgBox = true;-->
<!--  }-->

<!--  public hideMsgBox(): void {-->
<!--    this.showMsgBox = false;-->
<!--  }-->
<!--}-->
<!--</script>-->

<!--<style scoped lang="scss">-->
<!--.server-card {-->
<!--  padding: 1rem;-->
<!--  border-radius: 5px;-->
<!--  background: rgba(white, 0.04);-->
<!--  margin-bottom: 1rem;-->
<!--  display: flex;-->

<!--  .body {-->
<!--    flex: 1;-->
<!--    display: flex;-->
<!--    align-items: center;-->
<!--    justify-content: space-between;-->
<!--    margin-left: 2rem;-->

<!--    .buttons {-->
<!--      display: flex;-->
<!--    }-->
<!--  }-->
<!--}-->

<!--.max-2-lines {-->
<!--  overflow: hidden;-->
<!--  text-overflow: ellipsis;-->
<!--  display: -webkit-box;-->
<!--  -webkit-line-clamp: 2;-->
<!--  -webkit-box-orient: vertical;-->
<!--}-->
<!--</style>-->
