<template>
  <div class="card-list w-full" style="height: 128px;">
    <div style="height: 100%" class="flex flex-row my-4">
      <article :class="`relative overflow-hidden shadow-lg`" style="height: 100%; width: 128px;">
        <img class="rounded-sm" width="128"
             :src="server.protoResponse && server.protoResponse.favicon !== undefined && server.protoResponse.favicon.length > 0 ? server.protoResponse.favicon: art !== undefined && art.length > 0 ? art : defaultImage"
             alt="placeholder"
        />
        <div class="content">
        </div>
      </article>
      <div class="flex-1 p-2 bg-background-lighten flex flex-col">
        <div class="flex flex-row">
          <div class="name-box font-bold">{{server.name}}</div>
          <div class="ml-auto mr-2 text-sm text-gray-500" v-if="server.protoResponse && server.protoResponse.players">
            Players
            <span>{{server.protoResponse.players.online}}/{{server.protoResponse.players.max}}</span>
          </div>
        </div>

        <p class="mb-auto max-2-lines" v-if="server.protoResponse && server.protoResponse.description !== undefined">
          {{server.protoResponse.description.text.replace(/\u00a7[0-9a-fk-or]/ig, '')}}</p>
        <!-- <div  v-if="tags" class="flex flex-row items-center">
            <div class="flex flex-row">
                <span v-for="(tag, i) in limitedTags" :key="`tag-${i}`" @click="clickTag(tag.name)" class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">{{tag.name}}</span>
                <span v-if="tags.length > 5" :key="`tag-more`" class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light" style="font-variant: small-caps;">+{{tags.length - 5}}</span>
            </div>
        </div> -->
      </div>
      <div style="width:50px;" class="flex flex-col list-action-button-holder">
        <FTBButton :isRounded="false" color="primary"
                   class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-tr">
          <font-awesome-icon icon="play" size="sm" class="cursor-pointer"/>
          <p>Join</p></FTBButton>
        <FTBButton :isRounded="false" color="info"
                   class="list-action-button py-2 px-4 h-full text-center flex flex-col items-center justify-center rounded-br">
          <font-awesome-icon icon="ellipsis-h" size="sm" class="cursor-pointer"/>
          <p>More</p></FTBButton>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
    import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
    import FTBButton from '@/components/FTBButton.vue';
    // @ts-ignore
    import placeholderImage from '@/assets/placeholder_art.png';
    import {logVerbose} from '@/utils';

    const namespace: string = 'websocket';

    export interface MsgBox {
        title: string;
        content: string;
        type: string;
        okAction: () => void;
        cancelAction: () => void;
    }

    @Component({
        components: {
            FTBButton,
        },
        props: [
            'server',
            'art',
        ],
    })
    export default class ServerCard extends Vue {

    }
</script>

<style scoped lang="scss">
  .card {
    position: relative;
  }

  .pack-image {
    transition: filter .5s;
    height: 100%;
    object-fit: cover;
  }

  .card-list .list-action-button {
    filter: brightness(0.7);
  }

  .card-list:hover .list-action-button {
    filter: brightness(1);
  }

  .pack-image.blur {
    filter: blur(5px) brightness(50%);
  }

  .list-action-button-holder:hover .list-action-button:not(:hover) {
    height: 0;
    padding-top: 0;
    padding-bottom: 0;

    & > * {
      height: 0;
    }
  }

  .list-action-button {
    transition: height .2s, filter .4s;
    overflow: hidden;

    & > * {
      overflow: hidden;
    }

    & p:not(.cursor-pointer) {
      display: none;
    }
  }

  .list-action-button-holder .list-action-button:hover, .list-action-button-holder .list-action-button:hover ~ .list-action-button:hover {
    height: 100%;

    & p:not(.cursor-pointer) {
      display: block;
    }
  }

  .buttons {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
  }

  .row {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  .content {
    position: absolute;
    height: 100%;
    bottom: 0;
    width: 100%;
    color: #fff;
    opacity: 1;
    transition: opacity .3s;
    z-index: 2;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: flex-end;
  }

  .content .name-box {
    background: rgba(0, 0, 0, 0.6);
    width: 100%;
    text-align: left;
    font-size: 15px;
    font-weight: 700;
    padding: 2px 2px 2px 6px;
  }

  .content .update-box {
    background: rgba(255, 193, 7, 0.9);
    width: 100%;
    text-align: left;
    color: #000;
    font-weight: 700;
    padding: 2px 2px 2px 6px;
    top: 0;
    position: absolute;
  }

  .hoverContent {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 100%;
    height: 100%;
    color: #fff;
    opacity: 0;
    transition: opacity .5s;
    z-index: 2;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    // margin: auto 0;
  }

  .divider {
    height: 20px;
    border-left: 1px solid #fff;
    border-right: 1px solid #fff;
    margin: 0 20px;
  }

  .button {
    transition: transform .2s ease-in;
  }

  .hover-scale:hover {
    transform: scale(1.3);
  }

  .button-shadow {
    // text-shadow: 3px 6px #272634;
    filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, .8));
  }

  .max-2-lines {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
</style>
