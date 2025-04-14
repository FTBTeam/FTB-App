<script lang="ts" setup>
import appPlatform from '@platform';
import {DiscordWidget} from '@/types.ts';

import BhLogo from '@/assets/images/branding/bh-logo.svg';
import {logger} from '@/core/logger.ts';
import {
  faDiscord, faFacebook, faGithub,
  faInstagram,
  faTwitch,
  faXTwitter,
  faYoutube,
  IconDefinition,
} from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { onMounted, ref } from 'vue';
import { faBook, faBox, faCubesStacked, faRocket } from '@fortawesome/free-solid-svg-icons';
import { topicBuilder } from '@/views/supportTopicBuilder.ts';

const mkSocial = (name: string, icon: IconDefinition, goEndpoint: string) => ({
  name,
  icon,
  link: `https://go.ftb.team/${goEndpoint}`,
});

const socials = [
  mkSocial('FTB Discord', faDiscord, 'discord'),
  mkSocial('FTB Twitter', faXTwitter, 'twitter'),
  mkSocial('FTB Instagram', faInstagram, 'instagram'),
  mkSocial('FTB Twitch', faTwitch, 'twitch'),
  mkSocial('FTB Youtube', faYoutube, 'youtube'),
  mkSocial('FTB Facebook', faFacebook, 'facebook'),
  mkSocial('FTB Github', faGithub, 'github'),
];

const topicList = [
  {
    title: 'Trackers',
    items: [
      topicBuilder("App support", 'Something not working in the app? Want to make a feature request? Report it on our Github!')
        .goEndpoint("app-feedback")
        .icon(faRocket)
        .build(),
      topicBuilder("Modpack support", 'Looking to report an issue with one of our Modpacks or wanna suggest something cool? This is the place for you.')
        .goEndpoint("support-modpack")
        .icon(faCubesStacked)
        .build(),
      topicBuilder("Mod support", 'Found an issue with an *FTB* Mod? Let us know and we\'ll get it sorted out.')
        .goEndpoint("support-mod-issue")
        .icon(faBox)
        .build(),
    ],
  },
  {
    title: 'Guides',
    items: [
      topicBuilder("BisectHosting", "Get a top-notch Minecraft Server with our partner BisectHosting. With 24/7/365 support, 2,000+ Minecraft modpacks, and hosting for over 70+ games, you're in good hands.")
        .url("https://bisecthosting.com/ftb?r=app-support")
        .customIcon(BhLogo)
        .build(),
      topicBuilder("App Guides", 'Here you can find some useful guides on how to use the app, debugging support with common problems and steps on how to do things like setting up a server and backing up your game.')
        .goEndpoint("app-support")
        .icon(faBook)
        .build(),
    ],
  },
];

const discordWidgetData = ref<DiscordWidget | null>(null);

onMounted(async () => {
  // Fetch the widget data from discord, we don't care if this doesn't load so just log and move on
  try {
    const req = await fetch('https://discord.com/api/guilds/372448486723158016/widget.json');
    discordWidgetData.value = (await req.json()) as DiscordWidget;
  } catch (error: any) {
    logger.error('Failed to load discord data, maybe their api is offline?', error);
  }
})
</script>

<template>
  <div class="px-6 py-4">
    <div class="flex justify-between mb-6">
      <div class="main-head">
        <h1 class="font-bold text-2xl">Support</h1>
        <p class="text-muted">Looking for a little help? We're here for you!</p>
      </div>

      <div class="socials flex items-center gap-4">
        <div
          v-for="social in socials"
          class="item p-0-5 opacity-75 hover:opacity-100 ease-in-out duration-200 transition-opacity"
          :aria-label="social.name"
          data-balloon-pos="down-right"
          @click="() => appPlatform.utils.openUrl(social.link)"
        >
          <FontAwesomeIcon :icon="social.icon" />
        </div>
      </div>
    </div>

    <div class="discord-callout">
      <div class="body">
        <h2 class="font-bold text-xl mb-4">
          <FontAwesomeIcon class="mr-4" :icon="['fab', 'discord']" /> Give our Discord a try
        </h2>
        <p>
          Our Discord server is typically a great first place to ask questions and find answers to common questions or
          issues. Why not give it a go? We've got thousands of active members and our team is always lurking around.
        </p>
      </div>
      <div class="discord-btn" @click="appPlatform.utils.openUrl('https://go.ftb.team/discord')">
        <span>Join Discord</span>
        <span v-if="discordWidgetData">·</span>
        <span v-if="discordWidgetData">{{ discordWidgetData.presence_count.toLocaleString() }} online</span>
      </div>
    </div>

    <div class="support-area">
      <div class="topic-collection" v-for="(topic, index) in topicList" :key="index">
        <div class="name">{{ topic.title }}</div>
        <div class="items">
          <div
            class="item"
            v-for="(topicItem, i) in topic.items"
            :key="i"
            @click="appPlatform.utils.openUrl(topicItem.link)"
          >
            <div class="icon">
              <FontAwesomeIcon v-if="topicItem.icon" fixedWidth :icon="topicItem.icon" />
              <img v-if="topicItem.customIcon" :src="topicItem.customIcon" alt="Icon" />
            </div>
            <div class="body">
              <h2>{{ topicItem.name }}</h2>
              <p>{{ topicItem.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.support-area {
  display: grid;
  gap: 1.5rem;

  .topic-collection {
    .name {
      font-size: 1.15rem;
      font-weight: bold;
      margin-bottom: 1rem;
    }

    .items {
      .item {
        padding: calc(1rem - 2px);
        margin-bottom: 1.2rem;
        background-color: #393939;
        border-radius: 5px;
        display: grid;
        grid-template-columns: auto 1fr;
        align-items: center;
        cursor: pointer;
        border: 2px solid transparent;
        transition: 0.15s ease-in-out border-color, 0.15s ease-in-out background-color, 0.15s ease-in-out box-shadow,
          0.15s ease-in-out transform;

        &:hover {
          transform: translateY(-3px);
          box-shadow: 0 2px 20px rgba(black, 0.2);
          border-color: #729e4f;
          background-color: #282927;
        }

        .icon {
          width: 40px;
          margin: 0 1.5rem 0 0.5rem;
          font-size: 1.2rem;
          display: flex;
          justify-content: center;
        }

        img {
          margin: 0 auto;
          max-height: 50px;
        }

        h2 {
          font-weight: bold;
          margin-bottom: 0.1rem;
        }

        p {
          opacity: 0.8;
          line-height: 1.6em;
          padding-right: 0.5rem;
        }
      }
    }
  }
}

.discord-callout {
  padding: 2rem;
  background-size: 150%;
  background: black url('../assets/backgrounds/discord-callout-bg.svg') no-repeat center bottom -80px;
  box-shadow: 0 5px 25px 5px rgba(black, 0.3);
  border-radius: 5px;
  margin-bottom: 2rem;
  text-align: center;

  p {
    opacity: 0.95;
  }

  .discord-btn {
    background-color: #5865f2;
    display: inline-flex;
    padding: 0.7rem 1.5rem;
    margin-top: 1.5rem;
    gap: 1rem;
    border-radius: 5px;
    cursor: pointer;
    transition: 0.15s ease-in-out background-color, 0.15s ease-in-out box-shadow, 0.15s ease-in-out transform;
    font-weight: bold;

    &:hover {
      background-color: #8891f2;
      box-shadow: 0 2px 9px 0 rgba(136, 145, 242, 0.44);
      transform: translate(0px, -2px);
    }
  }
}
</style>
