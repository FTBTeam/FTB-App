<script setup lang="ts">
import {InstanceSettingsValue} from "@/components/groups/settings/InstanceSettings.vue";
import RamSlider from "@/components/groups/modpack/components/RamSlider.vue";
import {faChevronDown, faFolder} from "@fortawesome/free-solid-svg-icons";
import {UiButton} from "@/components/ui";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import AbstractInput from "@/components/ui/form/AbstractInput.vue";
import {onMounted, ref} from "vue";
import {JavaInstall} from "@/core/types/javaApi";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import appPlatform from "@platform";
import {alertController} from "@/core/controllers/alertController.ts";

const value = defineModel<InstanceSettingsValue["java"]>()

const javaVersions = ref<JavaInstall[]>([]);

// const jreSelection = ref('');
// if (!instance.embeddedJre) {
//   // Java version not in our list, thus it must be custom so flag it as custom
//   if (!javas.javas.find((e) =>  e.path === instance.jrePath)) {
//     jreSelection.value = '-1';
//   } else {
//     jreSelection.value = instance.jrePath;
//   }
// }

onMounted(async () => {
  const javas = await sendMessage("getJavas");
  javaVersions.value = javas.javas;
});

function browseForJava() {  
  appPlatform.io.selectFileDialog(null, (path) => {
    if (!value.value) {
      return;
    }
    
    if (typeof path !== 'undefined' && path == null) {
      alertController.error('Unable to set Java location as the path was not found')
      return;
    } else if (!path) {
      return;
    }

    const javaVersion = javaVersions.value.find((e) => e.path === path);
    if (javaVersion) {
      value.value.javaPath = javaVersion.path;
    }
  });
}
</script>

<template>
  <div v-if="value">
    <ram-slider class="mb-6" v-model="value.ram" />

    <section v-if="javaVersions" class="flex-1 mb-4">
      <AbstractInput label="Java version">
        <template v-slot="{ class: clazz }">
          <div class="relative">
            <select :class="clazz" class="appearance-none w-full" v-model="value.javaPath">
              <option value="-1" v-if="value.javaPath === '-1'" disabled>
                Custom selection ({{ value.javaPath }})
              </option>
              <option
                v-for="index in Object.keys(javaVersions)"
                :value="(javaVersions as any)[index].path"
                :key="(javaVersions as any)[index].name"
              >
                {{ (javaVersions as any)[index].name }}
              </option>
            </select>

            <div class="absolute z-[1] top-1/2 -translate-1/2 right-1">
              <FontAwesomeIcon :icon="faChevronDown" />
            </div>
          </div>
        </template>

        <template #suffix>
          <UiButton type="success" :icon="faFolder" @click="browseForJava">Browse</UiButton>
        </template>
      </AbstractInput>
    </section>
  </div>
</template>