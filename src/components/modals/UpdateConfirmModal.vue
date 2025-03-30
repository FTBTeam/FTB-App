<template>
  <modal
    :open="open"
    :title="`Update ${localInstance.version} to ${latestVersion.name}`"
    sub-title="Are you sure you want to update to the latest version of this pack?"
    :externalContents="true"
    @closed="close"
  >
    <modal-body>
      <message type="danger" class="mb-4" v-if="isUnstable">
        <p>
          <b class="font-bold">WARNING!</b> You're about to update to an unstable version of the pack. Bugs are
          expected and likely.
        </p>
      </message>

      <loader key="changes-loading" v-if="loadingChanges" title="Fetching Changelog" />
      <div key="changes-loading" v-else>
        <p>
          This version comes with the following changes, if you are sure you want to update, click confirm, otherwise
          close this message.
        </p>

        <message v-if="changes === null" key="changes-available" type="danger" class="mt-6">
          Unable to find a changelog for {{ latestVersion.name }}... this doesn't seem right 🤔. You can still update
          even if the changelog isn't loading.
        </message>

        <div v-else class="wysiwyg changes mt-6" v-html="parseMarkdown(changes)" />
      </div>
    </modal-body>
    <modal-footer class="flex justify-end">
      <ui-button :wider="true" icon="check" @click="update" type="success">
        Confirm
      </ui-button>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import Loader from '@/components/ui/Loader.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {parseMarkdown} from '@/utils';
import {Versions} from '@/modules/modpacks/types';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {alertController} from '@/core/controllers/alertController';
import {createLogger} from '@/core/logger';

@Component({
  methods: {parseMarkdown},
  components: {UiButton, Loader}
})
export default class UpdateConfirmModal extends Vue {
  private logger = createLogger("UpdateConfirmModal.vue");
  
  @Prop() open!: boolean;
  @Prop() localInstance!: InstanceJson | SugaredInstanceJson;
  @Prop() latestVersion!: Versions;

  isUnstable = false;
  showConfirm = false;

  loadingChanges = false;
  changes: string | null = null;
  
  @Emit() close() {
    this.isUnstable = false;
    this.showConfirm = false;
    this.changes = null;
  }
  
  @Watch('open')
  onOpen() {
    if (!this.open) {
      return;
    }
    
    if (!this.latestVersion || !this.localInstance) {
      this.logger.error("No latest version or local instance provided, can't open update modal")
      return;
    }
    
    this.isUnstable = this.latestVersion?.type.toLowerCase() !== 'release';
    
    this.loadChanges()
      .then(() => (this.loadingChanges = true))
      .catch(e => this.logger.error("Failed to load changes", e))
      .finally(() => (this.loadingChanges = false));
  }

  update() {
    if (!this.latestVersion) {
      this.logger.error("No latest version provided, can't update")
      return;
    }
    
    instanceInstallController.requestUpdate(
      this.localInstance,
      this.latestVersion,
      typeIdToProvider(this.localInstance.packType)
    )
    
    this.close();
  }

  async loadChanges() {
    if (typeIdToProvider(this.localInstance.packType) !== "modpacksch") {
      return;
    }

    if (!this.latestVersion) {
      this.logger.error("No latest version provided, can't load changes")
      return;
    }

    try {
      const packReq = await modpackApi.modpacks.getChangelog(this.localInstance.id, this.latestVersion.id, "modpacksch")
      this.changes = packReq?.content ?? ""
    } catch (e) {
      alertController.error("Failed to load changelog...")
    } // TODO: (M#01) (blocked by api) Support CF Packs
  }
}
</script>

<style scoped>

</style>