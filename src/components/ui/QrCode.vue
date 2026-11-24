<script setup lang="ts">
import encodeQR, {QrOpts} from 'qr';
import {onMounted, ref, watch} from "vue";
import {faCircleNotch} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

const {
  input,
  opts = {}
} = defineProps<{
  input: string;
  opts?: QrOpts
}>()

const loading = ref(false)
const code = ref<string>();
const error = ref<string | null>(null)

onMounted(() => {
  generateCode(input)
})

watch(() => input, (value) => {
  generateCode(value)
})

function generateCode(value: string) {
  if (value === "") {
    // Just don't generate anything.
    return;
  }

  loading.value = true
  code.value = "";
  try {
    code.value = encodeQR(value, 'svg', opts);
  } catch (e) {
    error.value = `Failed to generate QR code due to ${(e as Error).message}`
    console.log(e) // TODO: use logger?
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="w-46 h-46 bg-white rounded-lg p-2 flex items-center justify-center">
    <div class="w-full h-full" v-html="code" v-if="code"></div>
    <FontAwesomeIcon :icon="faCircleNotch" v-if="loading" />
  </div>
</template>

<style scoped>

</style>