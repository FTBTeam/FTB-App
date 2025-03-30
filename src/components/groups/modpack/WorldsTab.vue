<template>
  <div class="worldsTab max-w-4xl mx-auto">
    <div class="flex items-center flex-col mb-6">
      <img class="mb-2" src="../../../assets/images/branding/ftb-worlds-logo.png" alt="FTB Worlds Logo" width="300"/>
      <div class="mb-4">
        <h1 class="text-xl font-bold sm:max-w-lg text-center mb-4">Start your own, private, server with FTB Worlds and enjoy
          '{{ apiPack.name }}' with friends</h1>
        
        <p class="text-center text-sm opacity-75">Explore our pricing and features below</p>
      </div>
    </div>
    <div class="bg-sidebar-item p-6 rounded-lg mb-8">
      <div class="flex gap-10">
        <div class="w-1/4 flex">
          <div>
            <p class="font-black text-4xl font-mono">$20</p>
            <p class="opacity-75 text-sm">per month</p>
          </div>
        </div>
        <div class="flex-1 flex flex-col gap-4">
          <div v-for="(item, index) in priceFeatures" :key="index">
            <h2 class="font-bold mb-1">{{ item.title }}</h2>
            <p class="text-sm opacity-75">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6 mb-8">
      <p class="font-bold mb-2 text-xl">Getting started</p>
      <p class="mb-2">Getting your own FTB Worlds server couldn't be simpler! Just load up {{ apiPack.name }}, click on the FTB Worlds logo, and follow the instructions to get your server up and running in minutes.</p>
      <p>Learn more about FTB Worlds on our <a class="text-blue-400 hover:text-blue-200 underline" href="https://worlds.feed-the-beast.com">website</a>.</p>
    </div>

    <h2 class="text-xl font-bold mb-4">Features</h2>
    <div class="grid grid-cols-2 md:grid-cols-3 gap-6">
      <div v-for="(item, index) in features" :key="index" class="bg-sidebar-item rounded-lg p-6">
        <span class="bg-blue-600 p-3 rounded-xl inline-block mb-6">
          <font-awesome-icon fixedWidth :icon="item.icon" size="lg"/>
        </span>

        <h2 class="font-bold text-lg mb-2">{{ item.title }}</h2>
        <p>{{ item.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {ModPack} from '@/modules/modpacks/types';
import {faCheckDouble, faGlobe, faLock, faSave, faSliders, faUpload} from '@fortawesome/free-solid-svg-icons';

const features = [{
  title: 'Multiple worlds',
  desc: 'Ability to switch between different worlds on the fly (requires restart)',
  icon: faGlobe
}, {
  title: 'Backups',
  desc: 'Automated backups to keep your worlds safe',
  icon: faSave
}, {
  title: 'In-game panel',
  desc: 'Manage your server, backups, and invites from within the game',
  icon: faSliders
}, {
  title: 'High availability',
  desc: 'Always online, just when you or your friends need it',
  icon: faCheckDouble
}, {
  title: 'World importing',
  desc: 'Easy uploading of your single player world to hit the ground running',
  icon: faUpload
}, {
  title: 'Private by design',
  desc: 'Private servers for just you and your friends. Create invites and share the code to grant access to your world to others',
  icon: faLock
}];

const priceFeatures = [{
  title: 'Setup in minutes',
  desc: 'Easy, automated setup of your server in minutes'
}, {
  title: '2 world slots',
  desc: 'Switch between up to two world slots(saves)'
}, {
  title: 'Automatic backups',
  desc: '3 days worth of continuous, automated backups'
}, {
  title: 'Top quality modpacks',
  desc: 'A selection of the best FTB modpacks'
}];

@Component
export default class WorldsTab extends Vue {
  @Prop() instance?: SugaredInstanceJson;
  @Prop() apiPack!: ModPack;

  features = features;
  priceFeatures = priceFeatures;
}
</script>

<style lang="scss" scoped>

</style>