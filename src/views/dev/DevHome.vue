<script lang="ts" setup>

import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faGlobe } from '@fortawesome/free-solid-svg-icons';
</script>

<template>
  <router-link to="/dev/globals"><FontAwesomeIcon :icon="faGlobe" /> Globals</router-link>
</template>