<template>
  <div class="update" v-if="instance && localInstance && !isLatestVersion">
    <ftb-button color="warning" class="update-btn px-4 py-1" @click="update">
      <span class="hidden sm:inline-block">Update available</span>
      <font-awesome-icon icon="cloud-download-alt" class="sm:ml-2" />
    </ftb-button>

    <modal
      :open="showConfirm"
      :title="`Update to ${latestVersion.name}`"
      sub-title="Are you sure you want to update to the latest version of this pack?"
      :externalContents="true"
      @closed="closeModal"
    >
      <modal-body>
        <message type="danger" class="mb-4" v-if="isUnstable">
          <p>
            <b class="font-bold">WARNING!</b> You're about to update to an unstable version of the pack. Bugs are
            expected and likely.
          </p>
        </message>

        <loading2 key="changes-loading" v-if="loadingChanges" title="Fetching Changelog" />
        <div key="changes-loading" v-else>
          <p>
            This version comes with the following changes, if you are sure you want to update, click confirm, otherwise
            close this message.
          </p>

          <message v-if="changes === null" key="changes-available" type="danger" class="mt-6">
            Unable to find a changelog for {{ latestVersion.name }}... this doesn't seem right 🤔. You can still update
            even if the changelog isn't loading.
          </message>

          <vue-showdown
            class="changes mt-6 wysiwyg"
            v-else
            key="changes-available"
            :markdown="changes"
            flavor="github"
          />
        </div>
      </modal-body>
      <modal-footer class="flex justify-end">
        <ftb-button class="py-2 px-8" color="primary" css-class="text-center text-l" @click="update(true)">
          <font-awesome-icon icon="check" class="mr-2" size="1x" />
          Confirm
        </ftb-button>
      </modal-footer>
    </modal>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { Prop } from 'vue-property-decorator';
import { Instance, ModPack } from '@/modules/modpacks/types';
import Component from 'vue-class-component';
import Loading from '@/components/atoms/Loading.vue';
import Loading2 from '@/components/atoms/Loading2.vue';
import { createModpackchUrl } from '@/utils';

@Component({
  components: { Loading2, Loading },
})
export default class PackUpdateButton extends Vue {
  @Prop() instance!: ModPack;
  @Prop() localInstance!: Instance;

  isUnstable = false;
  showConfirm = false;

  loadingChanges = false;
  changes: string | null = null;

  update(force = false) {
    if (!this.latestVersion) {
      // How?
      return;
    }

    if (!force) {
      this.loadChanges()
        .then(() => (this.loadingChanges = true))
        .catch(console.error)
        .finally(() => (this.loadingChanges = false));
      this.isUnstable = this.latestVersion?.type.toLowerCase() !== 'release';
      this.showConfirm = true;
      return;
    }

    this.$emit('update');
    this.closeModal();
  }

  async loadChanges() {
    if (!this.latestVersion) {
      return; // how?
    }

    const packReq = await fetch(
      createModpackchUrl(
        `/${this.localInstance.packType === 0 ? 'modpack' : 'curseforge'}/${this.instance.id}/${
          this.latestVersion?.id
        }/changelog`,
      ),
    );

    this.changes = (await packReq.json()).content;
  }

  closeModal() {
    this.isUnstable = false;
    this.showConfirm = false;
    this.changes = null;
  }

  get isLatestVersion() {
    if (!this.instance?.versions.length) {
      return true;
    }

    // Use the Id's to decide on latest
    const newestVersion = this.latestVersion;
    if (!newestVersion) {
      return true;
    }

    // Again, using the ID to define if a version is newer. Should be fine?
    return this.localInstance.versionId >= newestVersion.id;
  }

  get latestVersion() {
    const versions = [...this.instance.versions];

    return versions.sort((a, b) => b.id - a.id).find((e) => e.type.toLowerCase() !== 'archived');
  }
}
</script>

<style lang="scss" scoped>
.update-btn {
  position: relative;

  box-shadow: 0 0 0 0 rgba(#ff801e, 1);
  animation: pulse 1.8s ease-in-out infinite;

  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0.7);
    }

    70% {
      box-shadow: 0 0 0 10px rgba(#ff801e, 0);
    }

    100% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0);
    }
  }
}

.changes {
  border-top: 2px solid rgb(white, 0.05);
  padding-top: 1rem;
}
</style>
