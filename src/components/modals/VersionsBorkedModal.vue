<template>
  <div>
    <modal-body>
      <p v-if="fixedVersion">
        The version of the modpack you are currently on has been archived. This means something has gone wrong with this
        version in an unrecoverable way. In this instance, we recommend you
        {{ isDowngrade ? 'downgrade' : 'update' }} the modpack.
      </p>

      <p v-else>
        The version of the modpack you're currently on has been archived. Unfortunately we do not currently have a safe
        version to update or downgrade to. In this case, we recommend you update as soon as an update is available
        (Which should be very soon).
        <span class="block mb-2" />
        You can continue to play the pack but note it may be unstable or not able to start.
      </p>

      <div class="notification mt-4" v-if="notification">
        <b class="font-bold mb-1 block">Message from developer</b>
        <p>{{ notification }}</p>
      </div>
    </modal-body>
    <modal-footer class="flex justify-end">
      <ftb-button
        class="py-2 px-4"
        :color="fixedVersion != null ? 'danger' : 'warning'"
        css-class="text-center text-l"
        @click="$emit('closed')"
      >
        <font-awesome-icon icon="times" class="mr-2" size="1x" />
        {{ fixedVersion !== null ? 'Ignore' : 'Close' }}
      </ftb-button>
      <ftb-button
        v-if="fixedVersion != null"
        class="py-2 px-8 ml-4"
        color="primary"
        css-class="text-center text-l"
        @click="$emit('action', fixedVersion)"
      >
        <font-awesome-icon icon="check" class="mr-2" size="1x" />
        {{ isDowngrade ? 'Downgrade pack' : 'Update pack' }}
      </ftb-button>
    </modal-footer>
  </div>
</template>

<script lang="ts">

@Component
export default class VersionsBorkedModal extends Vue {
  @Prop({ default: true }) isDowngrade!: boolean;
  @Prop() fixedVersion!: number | null;
  @Prop() notification!: string;
}
</script>
