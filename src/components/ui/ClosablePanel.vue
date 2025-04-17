<script lang="ts" setup>
// TODO: (M#02) Remove this component and replace it with a modal that support full screen
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { onMounted, onUnmounted, ref } from 'vue';
import appPlatform from '@platform'
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { useAds } from '@/composables/useAds.ts';

const { subtitle = '', scrollable = true } = defineProps<{
  open: boolean;
  title: string;
  subtitle?: string;
  scrollable?: boolean;
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const ads = useAds()
const isMac = ref(false);

onMounted(async () => {
  document.addEventListener('keydown', onEsc)
  isMac.value = await appPlatform.utils.getOsType() === "mac"
})

onUnmounted(() => {
  document.removeEventListener('keydown', onEsc)
})

function onEsc(event: any) {
  if (event.key !== 'Escape') {
    return;
  }

  emit('close')
}
</script>

<template>
  <transition name="slide-in-out" :duration="250">
    <div v-if="open" class="closable-panel" :class="{'is-mac': isMac, ads: ads.adsEnabled }" @click.self="emit('close')" >
      <div class="panel-container">
        <div class="heading">
          <div class="main">
            <div class="title">{{ title }}</div>
            <div class="sub-title">{{ subtitle }}</div>
          </div>
          <div class="closer" @click="emit('close')">
            Close
            <FontAwesomeIcon :icon="faTimes" />
          </div>
        </div>
        <div class="content" :class="{ scrollable }">
          <slot />
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped lang="scss">
.closable-panel {
  position: fixed;
  top: 2rem; // windows + linux title bar?
  bottom: 0;
  left: 0;

  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  background-color: rgba(black, .6);
  backdrop-filter: blur(5px);
  width: 100vw;
  
  &.ads {
    width: calc(100% - 400px);
  }

  &.is-mac {
    top: 1.8rem;
  }

  .system-frame & {
    top: 0;
  }

  .panel-container {
    background-color: var(--color-navbar);
    width: 95%;
    height: 95%;
    padding: 1rem 0;
    
    display: flex;
    flex-direction: column;
    border-radius: 8px;

    > .heading {
      display: flex;
      justify-content: space-between;
      margin-bottom: 1rem;
      padding: 0 1.5rem;

      .main {
        flex: 1;
        padding-right: 1rem;

        .title {
          font-size: 1.3rem;
          font-weight: 900;
          margin-bottom: 0.3rem;
        }

        .sub-title {
          opacity: 0.8;
          font-size: 0.875rem;
          font-weight: bold;
        }
      }

      .closer {
        cursor: pointer;
        padding-left: 2rem;
        display: flex;
        align-items: center;

        svg {
          margin-left: 0.5rem;
        }
      }
    }

    > .content {
      flex: 1;
      //max-height: 100%;
      overflow: auto;
      padding: 0 calc(1.5rem - .5rem) 2rem;
      margin: 0 .5rem;
    }
  }
  
  //position: fixed;
  //top: 1.5rem;
  //left: 1.5rem;
  //padding-top: 1.5rem;
  //width: calc((100% - 300px - (2.5rem)) - 3rem);
  //height: calc(100vh - 4rem);
  //border-radius: 10px;
  //z-index: 10000;
  //display: flex;
  //flex-direction: column;
  //background-color: #343434;
  //box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  //
  //&.overwolf {
  //  width: calc(100% - 400px);
  //}
  //
  //&.is-mac {
  //  top: calc(1.8rem + 1rem);
  //}
}

.slide-in-out-enter-active,
.slide-in-out-leave-active {
  transition: opacity 0.25s ease-in-out;
  
  .panel-container {
    transition: transform 0.25s cubic-bezier(.67,0,.2,1);
  }
}

.slide-in-out-enter,
.slide-in-out-leave-to {
  .panel-container {
    transform: translateY(30px);
  }
  
  opacity: 0;
}
</style>
