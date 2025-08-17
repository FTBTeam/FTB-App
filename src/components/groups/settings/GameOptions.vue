<script setup lang="ts">
import {InstanceSettingsValue} from "@/components/groups/settings/InstanceSettings.vue";

const value = defineModel<InstanceSettingsValue["game"]>()
</script>

<template>
  <div>
    {{ value }}
  </div>
</template>