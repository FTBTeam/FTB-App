<template>
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 251 150.66">
    <path fill="none" stroke="#fff" stroke-miterlimit="10" stroke-width="10" d="M7.5 5h237v111H7.5z"/>
    <text class="logo-text-colour" transform="translate(1.03 145.51)" font-size="29" font-family="Open Sans" font-weight="800" letter-spacing=".05em">FEED THE BEAST</text>
    <path class="logo-inner-text-colour" d="M68.5 74.94V85h-11V33h38v10h-27v11h23v10h-23zM122.5 43h-16v-9h43v9h-16v42h-11zM160.5 33h24c9.71 0 16.29 3.14 16.29 12.64 0 5.15-2.36 8.43-6.93 10.65 6.28 1.78 9.35 6.57 9.35 12.92 0 10.36-8.78 14.79-18 14.79H160.5zm11 21h11.42c4 0 6.9-1.8 6.9-6.11 0-4.81-3.73-5.89-7.76-5.89H171.5zm0 22h12c4.36 0 8.29-1.43 8.29-6.71S188.57 62 183.71 62H171.5z"/>
  </svg>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Logo extends Vue {
}
</script>

<style scoped>
.logo-text-colour{fill:#fff}
.logo-inner-text-colour{fill:#fff}

</style>