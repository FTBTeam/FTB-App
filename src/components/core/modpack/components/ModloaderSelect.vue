<template>
 <div class="modloaderSelect">
   <b class="mb-4 block">Select a Mod Loader</b>

   <div class="loaders mb-6">
     <div class="loader" :class="{active: userLoaderProvider === index}" v-for="(_, index) in availableLoaders" :key="index" @click="userLoaderProvider = index">
       {{ index | title }}
     </div>
   </div>

   <f-t-b-toggle v-if="!stringIsEmpty(userLoaderProvider)" :value="userUseLatestLoader" @change="v => userUseLatestLoader = v" label="Use latest Mod Loader version (recommended)" />

   <selection2
     v-if="!userUseLatestLoader && loaderVersions.length > 0"
     :open-up="true"
     label="Version"
     class="mb-4"
     :options="loaderVersions"
     :value="value"
      @input="input"
   />
 </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import Selection2, {SelectionOption} from '@/components/atoms/input/Selection2.vue';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {ModLoader, ModLoadersResponse} from '@/core/@types/modpacks/modloaders';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {JavaFetch} from '@/core/javaFetch';

@Component({
  methods: {stringIsEmpty},
  components: {FTBToggle, Selection2}
})
export default class ModloaderSelect extends Vue {
  @Prop() mcVersion!: string;
  @Prop() value!: string;
  @Emit("input") input(value: string) {}

  availableLoaders: Record<string, ModLoader[]> = {};
  loadingModloaders = false;
  
  userLoaderProvider = "";
  userUseLatestLoader = true;
  
  async mounted() {
    await this.loadLoaders();
  }
  
  @Watch("mcVersion")
  async onMcVersionChange() {
    await this.loadLoaders();
  }
  
  async loadLoaders() {
    await toggleBeforeAndAfter(() => this.loadAvailableLoaders(this.mcVersion), state => this.loadingModloaders = state);
  }
  
  async loadAvailableLoaders(mcVersion: string) {
    const knownLoaders = ["forge", "neoforge", "fabric"];
    const foundLoadersForVersion = {} as Record<string, ModLoader[]>;

    for (const loader of knownLoaders) {
      const request = await JavaFetch.modpacksCh(`loaders/${mcVersion}/${loader}`).execute()
      if (request?.status !== "success") {
        continue;
      }

      const loaderData = request.json<ModLoadersResponse>();
      if (loaderData.total === 0) {
        continue;
      }

      foundLoadersForVersion[loader] = loaderData.loaders.sort((a, b) => b.id - a.id);
    }

    this.availableLoaders = foundLoadersForVersion;
  }

  // Set the user loader to the first one available
  @Watch("userLoaderProvider")
  async onLoaderProviderChange(newVal: string, oldVal: string) {
    if (newVal === oldVal) {
      return;
    }

    this.input(this.loaderVersions[0].value);
  }

  get loaderVersions() {
    if (this.userLoaderProvider === "" || !this.availableLoaders[this.userLoaderProvider]) {
      return []
    }

    return this.availableLoaders[this.userLoaderProvider].map((e: any) => ({
      label: e.version,
      value: e.version,
      meta: e.type
    })) as SelectionOption[]
  }
}
</script>

<style lang="scss" scoped>
.loaders {
  display: flex;

  .loader {
    cursor: pointer;
    flex: 1;
    padding: 1rem 0;
    text-align: center;
    background-color: var(--color-navbar);
    transition: background-color .25s ease-in-out;
    font-weight: bold;

    &:hover {
      background-color: var(--color-light-primary-button);
    }

    &.active {
      background-color: var(--color-primary-button);
    }

    &:first-child {
      border-radius: 8px 0 0 8px;
    }

    &:last-child {
      border-radius: 0 8px 8px 0;
    }

    &:not(:last-child) {
      border-right: 1px solid rgba(white, .05);
    }
  }
}
</style>