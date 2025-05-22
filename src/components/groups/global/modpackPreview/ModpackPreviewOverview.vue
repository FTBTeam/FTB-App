<script lang="ts" setup>
import {ModPack} from "@/core/types/appTypes.ts";
import Stat from "@/components/groups/global/modpackPreview/Stat.vue";
import {computed} from "vue";
import {marked} from "marked";
import {resolveModloader} from "@/utils/helpers/packHelpers.ts";
import {prettyByteFormat} from "@/utils";

const {
  modpack
} = defineProps<{
  modpack: ModPack
}>()

const description = computed(() => marked.parse(modpack.description))
const poweredBy = computed(() => resolveModloader(modpack))
const minecraftVersion = computed(() => modpack.versions?.[0].targets.find(e => e.name === "minecraft")?.version ?? "Unknown")
const expectedRam = computed(() => modpack.versions?.[0].specs?.minimum ?? 1024 * 4)
</script>

<template>
  <div class="mb-6 flex flex-wrap gap-8">
    <Stat title="Powered by" :value="poweredBy" />
    <Stat title="For" :value="minecraftVersion" />
    <Stat title="Created by" :value="modpack.authors.map(e => e.name).join(', ')" />
    <Stat title="Expects" :value="`At least ${prettyByteFormat(expectedRam * (1024 * 1024))} RAM`" />
  </div>
  
  <suspense>
    <div class="wysiwyg helper-wysiwyg" v-html="description" />
    <template #fallback>
      Loading...
    </template>
  </suspense>
</template>

<style lang="scss">
.helper-wysiwyg img {
  border-radius: .5rem;
}
</style>