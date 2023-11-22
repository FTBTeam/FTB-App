<template>
 <div class="modloaderSelect">
   <message header="Optional" icon="info" type="info" class="mb-4" v-if="showOptional && hasAvailableLoaders">
     A Mod Loader will allow you to load mods into Minecraft. Each Mod Loader will have different mods available so we recommend looking at <a>CurseForge</a> to see what's available.
   </message>
   
   <Loader v-if="loadingModloaders" />
   
   <template v-else-if="hasAvailableLoaders">
     <b class="mb-4 block">Select a Mod Loader</b>

     <div class="loaders mb-6">
       <div class="loader" :class="{active: userLoaderProvider === index}" v-for="(_, index) in availableLoaders" :key="index" @click="userLoaderProvider = index">
         {{ index | title }}
       </div>
       <div class="loader" v-if="showNone" :class="{active: userLoaderProvider === ''}" @click="userLoaderProvider = ''">None</div>
     </div>

     <ui-toggle v-if="provideLatestOption && userLoaderProvider !== ''" class="mb-4" label="Use latest Mod Loader version (recommended)" desc="The latest version of each modloader is typically the most stable version" v-model="userUseLatestLoader" />

     <selection2
       v-if="userLoaderProvider !== ''"
       :open-up="true"
       label="Version"
       class="mb-4"
       :disabled="loaderVersions.length === 0 || (provideLatestOption && userUseLatestLoader)"
       :options="loaderVersions"
       v-model="userLoaderVersion"
       @change="select(userLoaderProvider, userLoaderVersion)"
     />
   </template>

   <message v-else header="No loaders" icon="exclamation" type="warning">
     Sadly we were not able to find any mod loaders for Minecraft {{ mcVersion }}. This is likely due to there being no mod loaders available just yet.
     <br><br>
     You can continue to create an instance without a mod loader and set one up later once one is available.
   </message>
 </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import Selection2, {SelectionOption} from '@/components/core/ui/Selection2.vue';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {ModLoadersResponse, ModLoaderWithPackId} from '@/core/@types/modpacks/modloaders';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {JavaFetch} from '@/core/javaFetch';
import Loader from '@/components/atoms/Loader.vue';
import UiToggle from '@/components/core/ui/UiToggle.vue';

@Component({
  methods: {stringIsEmpty},
  components: {UiToggle, Loader, Selection2}
})
export default class ModloaderSelect extends Vue {
  @Prop() mcVersion!: string;
  @Prop({default: true}) showNone!: boolean;
  @Prop({default: false}) showOptional!: boolean;
  
  @Prop({default: true}) provideLatestOption!: boolean;

  availableLoaders: Record<string, ModLoaderWithPackId[]> = {};
  loadingModloaders = false;
  
  userLoaderProvider = "";
  userUseLatestLoader = true;
  userLoaderVersion = "";
  
  @Emit() select(loader: string, version: string): [string, ModLoaderWithPackId] | null {
    const loaderProvider = this.availableLoaders[loader];
    if (!loaderProvider) {
      return null;
    }

    const modLoader = loaderProvider.find(e => e.version === version);
    if (!modLoader) {
      return null;
    }
    
    return [loader, modLoader];
  }
  
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
    const foundLoadersForVersion = {} as Record<string, ModLoaderWithPackId[]>;

    for (const loader of knownLoaders) {
      const request = await JavaFetch.modpacksCh(`loaders/${mcVersion}/${loader}`).execute()
      if (request?.status !== "success") {
        continue;
      }

      const loaderData = request.json<ModLoadersResponse>();
      if (loaderData.total === 0 || !loaderData.loaders) {
        continue;
      }
      
      foundLoadersForVersion[loader] = loaderData.loaders
        .sort((a, b) => b.id - a.id)
        .map(e => ({...e, packId: loaderData.id, logo: loaderData?.art?.find(e => e.type === "square")?.url ?? undefined}));
    }
    
    this.availableLoaders = foundLoadersForVersion;
  }

  // Set the user loader to the first one available
  @Watch("userLoaderProvider")
  async onLoaderProviderChange(newVal: string, oldVal: string) {
    if (newVal === oldVal) {
      return;
    }
    
    if (this.userLoaderProvider === "") {
      this.userLoaderVersion = "";
      return;
    }
    
    this.userLoaderVersion = this.loaderVersions[0]?.value ?? "";
    this.select(this.userLoaderProvider, this.userLoaderVersion)
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
  
  get hasAvailableLoaders() {
    return Object.keys(this.availableLoaders).length > 0;
  }
}
</script>

<style lang="scss" scoped>
.loaders {
  display: flex;

  .loader {
    cursor: pointer;
    flex: 1;
    padding: .8rem 0;
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