<script lang="ts" setup>
import appPlatform from '@platform'
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import { InstanceJson, JavaInstall, SugaredInstanceJson } from '@/core/types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';
import DuplicateInstanceModal from '@/components/modals/actions/DuplicateInstanceModal.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import {resolveModloader, resolveModLoaderVersion, typeIdToProvider} from '@/utils/helpers/packHelpers';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import {computeAspectRatio, megabyteSize, prettyByteFormat} from '@/utils';
import ModloaderSelect from '@/components/groups/modpack/components/ModloaderSelect.vue';
import {ModLoaderWithPackId} from '@/core/types/modpacks/modloaders';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, UiToggle, Selection2, UiButton, InputNumber, Input } from '@/components/ui';
import { toTitleCase } from '@/utils/helpers/stringHelpers.ts';
import { useInstallStore } from '@/store/installStore.ts';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import {
  faCopy,
  faDownload,
  faFolder, faLock,
  faPen, faTimes,
  faTrash,
  faUndo, faUnlock,
  faWarning,
  faWrench,
} from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';
import TextArea from '@/components/ui/form/TextArea/TextArea.vue';

const router = useRouter();
const appStore = useAppStore();
const installStore = useInstallStore();
const appSettingsStore = useAppSettings();

const {
  instance
} = defineProps<{
  instance: SugaredInstanceJson;
}>()

const emit = defineEmits<{
  (event: 'back'): void;
}>()

const preferIPv4Arg = "-Djava.net.preferIPv4Stack=true"

const instanceSettings = ref<SaveJson>({} as any);
const previousSettings = ref<SaveJson>({} as any);

const showDuplicate = ref(false);
const jreSelection = ref('');
const javaVersions = ref<JavaInstall[]>([]);
const deleting = ref(false);

const imageFile = ref<string | null>(null);
const resolutionId = ref("");

const userSelectModLoader = ref(false);
const userSelectedLoader = ref<[string, ModLoaderWithPackId] | null>(null);

onMounted(async () => {
  const javas = await sendMessage("getJavas");
  javaVersions.value = javas.javas;
  
  if (!instance.embeddedJre) {
    // Java version not in our list, thus it must be custom so flag it as custom
    if (!javas.javas.find((e) =>  e.path === instance.jrePath)) {
      jreSelection.value = '-1';
    } else {
      jreSelection.value = instance.jrePath;
    }
  }
  
  instanceSettings.value = createInstanceSettingsFromInstance(instance);
  previousSettings.value = { 
    ...instanceSettings.value
  }
})

function selectResolution(id: string | null) {
  if (id === null) return;
  
  const selected = appSettingsStore.systemHardware?.supportedResolutions.find(e => `${e.width}|${e.height}` === id);
  if (!selected) {
    return;
  }

  instanceSettings.value.width = selected.width;
  instanceSettings.value.height = selected.height;
  
  saveSettings();
}

function browseForJava() {
  appPlatform.io.selectFileDialog((path) => {
    if (typeof path !== 'undefined' && path == null) {
      alertController.error('Unable to set Java location as the path was not found')
      return;
    } else if (!path) {
      return;
    }

    const javaVersion = javaVersions.value.find((e) => e.path === path);
    jreSelection.value = !javaVersion ? '-1' : javaVersion.path;

    instanceSettings.value.jrePath = path;
    saveSettings();
  });
}

function updateJrePath(value: any) {
  instanceSettings.value.jrePath = value.target.value;
  saveSettings();
}

async function repairInstance() {
  if (!(await dialogsController.createConfirmationDialog("Are you sure?", "We will attempt to repair this instance by reinstalling the modpack around your existing files. Even though this shouldn't remove any of your data, we recommend you make a backup of this instance before continuing."))) {
    return;
  }

  await appStore.controllers.install.requestUpdate(instance, instance.versionId, typeIdToProvider(instance.packType));
  emit("back")
}

async function installModLoader() {
  if (!userSelectedLoader.value) {
    return;
  }
  
  const result = await sendMessage("instanceOverrideModLoader", {
    uuid: instance.uuid,
    modLoaderId: parseInt(userSelectedLoader.value[1].packId, 10),
    modLoaderVersion: userSelectedLoader.value[1].id,
  });  
  
  if (result.status !== "error") {
    userSelectModLoader.value = false;
    userSelectedLoader.value = null;

    if (result.status === "prepare") {
      installStore.addModloaderUpdate({
        instanceId: instance.uuid,
        packetId: result.requestId
      })
    }

    emit("back");
  }
}

async function toggleLock() {
  const newState = !instanceSettings.value.locked;
  if (!newState && !(await dialogsController.createConfirmationDialog("Are you sure?", "Unlocking this instance will allow you to add extra mods and modify the instance in other ways. This can allow for destructive actions!\n\nAre you sure you want to unlock this instance?"))) {
    return;
  }
  
  instanceSettings.value.locked = newState;
  await saveSettings(); 
}

async function saveSettings() {
  if (JSON.stringify(previousSettings.value) === JSON.stringify(instanceSettings.value)) {
    return;
  }
  
  const result = await InstanceController.from(instance)
    .updateInstance(instanceSettings.value);

  if (result) {
    alertController.success("Settings saved!")

    // Update the previous settings
    instanceSettings.value = createInstanceSettingsFromInstance(result.instanceJson)
    previousSettings.value = {
      ...instanceSettings.value
    }
  } else {
    alertController.error("Failed to save settings")
  }
}

async function browseInstance() {
  await gobbleError(async () => {
    if ("path" in instance) {
      await appPlatform.io.openFinder(instance.path)
      return;
    }
  })
}

function confirmDelete() {
  const dialogRef = dialogsController.createDialog(
    dialog("Are you sure?")
      .withContent(`Are you absolutely sure you want to delete \`${instance.name}\`! Doing this **WILL permanently** delete all mods, world saves, configurations, and all the rest... There is no way to recover this pack after deletion...`)
      .withType("warning")
      .withButton(button("Delete")
        .withAction(async () => {
          dialogRef.setWorking(true)
          await deleteInstance()
          dialogRef.close();
        })
        .withIcon(faTrash)
        .withType("error")
        .build())
      .build()
  )
}

function createInstanceSettingsFromInstance(instance: InstanceJson): SaveJson {
  resolutionId.value = resolutionList.value
    .find((e) => e.value === `${instance.width ?? ''}|${instance.height ?? ''}`)
    ?.value ?? "";

  return {
    name: instance.name,
    jvmArgs: instance.jvmArgs,
    jrePath: instance.jrePath,
    memory: instance.memory,
    width: instance.width,
    height: instance.height,
    fullScreen: instance.fullscreen,
    releaseChannel: instance.releaseChannel,
    category: instance.category,
    locked: instance.locked,
    shellArgs: instance.shellArgs,
    programArgs: instance.programArgs,
    preventMetaModInjection: instance.preventMetaModInjection,
  }
}

async function deleteInstance() {
  deleting.value = true;

  const controller = InstanceController.from(instance);
  await toggleBeforeAndAfter(() => controller.deleteInstance(), state => deleting.value = state);
  await gobbleError(() => router.push({
    name: RouterNames.ROOT_LIBRARY
  }));
}

function preferIPv4Clicked(event: any) {
  if (!event) {
    instanceSettings.value.jvmArgs = instanceSettings.value.jvmArgs.replace(preferIPv4Arg, "").trim()
  } else {
    instanceSettings.value.jvmArgs = `${instanceSettings.value.jvmArgs} ${preferIPv4Arg}`.trim()
  }
  
  saveSettings()
}

const prefersIPv4 = computed(() => instanceSettings.value?.jvmArgs?.includes(preferIPv4Arg))
const channelOptions = computed(() => ReleaseChannelOptions(true));
const hasModloader = computed(() => instance?.modLoader !== instance.mcVersion);
const resolutionList = computed(() => {
  const resList = [];
  resList.push({
    value: "",
    label: "Custom",
    meta: "Custom"
  });

  if (!appSettingsStore.systemHardware) {
    return resList;
  }
  
  for (const res of appSettingsStore.systemHardware?.supportedResolutions) {
    resList.push({
      value: `${res.width}|${res.height}`,
      label: `${res.width} x ${res.height}`,
      // Calculate the aspect ratio in the form of a 16:9 for example
      meta: computeAspectRatio(res.width, res.height)
    })
  }

  return resList;
});

watch(imageFile, (value) => {
  if (value) {
    instanceSettings.value.instanceImage = value
    saveSettings();
  }
})
</script>

<template>
  <div class="pack-settings">
    <ArtworkSelector :pack="instance" class="mb-4" v-model="imageFile" :allow-remove="false" />
    
    <Input
      label="Instance Name"
      v-model="instanceSettings.name"
      @blur="saveSettings"
      class="mb-4"
      fill
    />

    <UiButton :icon="instanceSettings.locked ? faUnlock : faLock" @click="toggleLock" class="mb-6">
      {{instanceSettings.locked ? 'Unlock' : 'Lock'}} instance
    </UiButton>
    
    <CategorySelector :open-down="true" class="mb-6" v-model="instanceSettings.category" @input="() => saveSettings()" />

    <div class="buttons flex gap-4 mb-8">
      <UiButton size="small" type="info" :icon="faFolder" @click="browseInstance()">
        Open Folder
      </UiButton>

      <UiButton size="small" :icon="faCopy" @click="showDuplicate = true" type="info" aria-label="Copy this instance to a new instance, mods, worlds and all">
        Duplicate
      </UiButton>
      
      <UiButton v-if="instance.id != -1" size="small" :icon="faWrench" type="warning" aria-label="Something not looking right? This might help!" @click="repairInstance">
        Repair
      </UiButton>
      
      <UiButton size="small" type="danger" :icon="faTrash" @click="confirmDelete">
        Delete instance
      </UiButton>
    </div>

    <ram-slider class="mb-6" v-model="instanceSettings.memory" @change="saveSettings" />
    
    <ui-toggle
      label="Fullscreen"
      desc="Always open Minecraft in Fullscreen mode"
      v-model="instanceSettings.fullScreen"
      @input="() => {
          saveSettings();
      }"
      class="mb-4"
      :align-right="true"
    />
    
    <div class="mb-6" :class="{'cursor-not-allowed opacity-50 pointer-events-none': instanceSettings.fullScreen}">
      <div class="flex items-center mb-4">
        <div class="block flex-1 mr-2">
          <b>Size presets</b>
          <small class="text-muted block mt-2">Select a preset based on your system</small>
        </div>
        
        <selection2
          v-if="resolutionList.length"
          v-model="resolutionId"
          @updated="selectResolution"
          :style="{width: '220px'}"
          :options="resolutionList"
        />
      </div>
      <div class="flex items-center mb-4">
        <div class="block flex-1 mr-2">
          <b>Width</b>
          <small class="text-muted block mt-2">The Minecraft windows screen width</small>
        </div>
        <InputNumber class="mb-0" v-model="instanceSettings.width" @blur="saveSettings" />
      </div>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Height</b>
          <small class="text-muted block mt-2">The Minecraft windows screen height</small>
        </div>
        <InputNumber class="mb-0" v-model="instanceSettings.height" @blur="saveSettings" />
      </div>
    </div>
    
    <h2 class="text-lg mb-4 font-bold text-warning">
      <FontAwesomeIcon :icon="faWarning" class="mr-2" />
      Advanced
    </h2>
    
    <div class="mb-8">
      <div class="flex items-center mb-6">
        <div class="block flex-1 mr-2">
          <b>Release Channel</b>
          <small class="block text-muted mr-6 mt-2">
            The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
        </div>

        <selection2
          :options="channelOptions"
          v-model="instanceSettings.releaseChannel"
          :style="{width: '192px'}"
          @change="() => saveSettings()"
        />
      </div>

      <div class="flex items-center mb-6">
        <div class="block flex-1 mr-2" :class="{'opacity-75': instance.locked}">
          <b>Modloader</b>
          <div v-if="instance.locked" class="mt-2 text-red-400">The instances Modloader can not be modified whilst the instance is locked.</div>
          <small class="block text-muted mr-6 mt-2">At any point you can update / down grade your mod loader for any modpack. This can sometimes be a destructive action and we recommend only doing this when you know what you're doing.</small>
          
          <div class="buttons flex gap-2 mt-4" v-if="!instance.locked">
            <UiButton v-if="!hasModloader" size="small" type="info" :icon="faDownload" @click="userSelectModLoader = true">Install Modloader</UiButton>
            <UiButton v-else size="small" type="info" :icon="faPen" @click="userSelectModLoader = true">Update Modloader</UiButton>
          </div>
        </div>
      </div>
      
      <ui-toggle
        :align-right="true"
        label="Disable helper mod injection"
        desc="The FTB App will inject helper mods into your instance to help the app and your instance work together. Sometimes this can cause issues with Minecraft, Mods, Etc. You can disable this behaviour here."
        v-model="instanceSettings.preventMetaModInjection"
        @input="saveSettings"
        class="mb-6" />

      <section class="flex-1 mb-4">
        <label class="block tracking-wide text-white-700 font-bold mb-2">Java Version</label>
        <div class="flex items-center gap-4">
          <select
            class="appearance-none block flex-1 bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded w-full"
            v-model="jreSelection"
            @change="updateJrePath"
          >
            <option value="-1" v-if="jreSelection === '-1'" disabled>
              Custom selection ({{ instanceSettings.jrePath }})
            </option>
            <option
              v-for="index in Object.keys(javaVersions)"
              :value="(javaVersions as any)[index].path"
              :key="(javaVersions as any)[index].name"
            >
              {{ (javaVersions as any)[index].name }}
            </option>
          </select>

          <UiButton type="success" :icon="faFolder" @click="browseForJava">Browse</UiButton>
        </div>
      </section>
      
      <div class="flex gap-4 flex-col mb-6">
        <TextArea
          label="Java runtime arguments"
          hint="These arguments are appended to your instances upon start, they are normal java arguments. New lines will be removed."
          placeholder="-TestArgument=120"
          v-model="instanceSettings.jvmArgs"
          @blur="() => {
            // Remove all new lines and trim the string
            instanceSettings.jvmArgs = instanceSettings.jvmArgs.trim().replaceAll(/(\r\n|\n|\r)/gm, '')
            saveSettings()
          }"
          :spellcheck="false"
          fill
          :rows="4"
        />
        
        <div class="flex gap-4">
          <UiButton size="small" :icon="faUndo" @click="() => {
            instanceSettings.jvmArgs = appSettingsStore.rootSettings?.instanceDefaults.javaArgs ?? ''
          }">
            Reset to Instance defaults
          </UiButton>

          <UiButton size="small" :icon="faUndo" @click="() => {
            instanceSettings.jvmArgs = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M'
          }">
            Reset to Vanilla defaults
          </UiButton>
        </div>
        
        <ui-toggle class="mb-4" label="Prefer IPv4 network requests" :value="prefersIPv4" @input="preferIPv4Clicked" />
      </div>

      <Input
        class="mb-6"
        label="Program arguments"
        :value="instanceSettings.programArgs"
        v-model="instanceSettings.programArgs"
        placeholder="--fullscreen"
        @blur="saveSettings"
        hint="These arguments are appended to the end of the java command, these are typically arguments Minecraft uses."
        fill
      />
      
      <Input
        class="mb-6"
        label="Shell arguments"
        :value="instanceSettings.shellArgs"
        v-model="instanceSettings.shellArgs"
        placeholder="/usr/local/application-wrapper"
        @blur="saveSettings"
        fill
        hint="These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing."
      />
      
      <p class="mb-2 font-bold">Startup preview</p>
      <p class="mb-4 block text-sm">This is for illustrative purposes only, this is not a complete example.</p>

      <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto select-text" v-if="instanceSettings.memory">
        {{instanceSettings.shellArgs}} java {{instanceSettings.jvmArgs}} -Xmx{{prettyByteFormat(Math.floor(parseInt(instanceSettings.memory.toString()) * megabyteSize))}} -jar minecraft.jar {{instanceSettings.programArgs}}
      </code>
    </div>

    <Modal
      :open="showDuplicate"
      :externalContents="true"
      @closed="showDuplicate = false"
      title="Duplicate Instance"
      subTitle="Are you sure?!"
    >
      <duplicate-instance-modal
        @finished="showDuplicate = false"
        :uuid="instance.uuid"
        :instanceName="instance.name"
        :category="instance.category"
      />
    </Modal>
    
    <Modal :open="userSelectModLoader" title="Select Modloader" :sub-title="`This instance is currently using ${hasModloader ? instance.modLoader : 'Vanilla'}`" @closed="() => {
      userSelectModLoader = false
      userSelectedLoader = null
    }">
      <div class="current mb-6 grid grid-cols-2 gap-y-2 wysiwyg items-center">
        <b>Current Modloader</b> <code>{{ toTitleCase(hasModloader ? resolveModloader(instance) : 'Vanilla') }}</code>
        <b>Current version</b> <code>{{resolveModLoaderVersion(instance)}}</code>
      </div>
      
      <p class="mb-2">You can select a Modloader to install or switch to using the selection below. Please be aware that if you are currently running a modpack, switching Modloader version may break the modpack.</p>
      <p class="mb-6">Switching between Modloader provider (aka: Forge -> Fabric) will <b>not</b> remove incompatible mods.</p>
      
      <modloader-select @select="e => userSelectedLoader = e" :mc-version="instance.mcVersion" :provide-latest-option="false" :show-none="false" />
      
      <template #footer>
        <div class="flex justify-end gap-4">
          <UiButton type="warning" :icon="faTimes" @click="() => {
            userSelectModLoader = false
            userSelectedLoader = null
          }">Close</UiButton>
          <UiButton type="success" :wider="true" :icon="faDownload" :disabled="userSelectedLoader === null" @click="() => installModLoader()">Install</UiButton>
        </div>
      </template>
    </Modal>
  </div>
</template>

<style scoped lang="scss">
.pack-settings {
  font-size: 14px;
}
</style>
