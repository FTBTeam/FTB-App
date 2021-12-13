<template>
  <div class="authentication" @mousedown.self="$emit('close')">
    <div class="body-contents text-center">

      <div class="main">
        <h3 class="text-2xl mb-4"><b>Your instance is loading</b></h3>
        <p class="mb-2">
          We are currently starting your instance, this may take a few seconds.
        </p>
        <ProgressBar class="my-10" />
      <ftb-button class="px-6 py-4 border-solid border-2 border-red-500 hover:border-danger hover:bg-danger" @click="$emit('close')">Cancel</ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import Loading from '@/components/atoms/Loading.vue';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import { Action } from 'vuex-class';
import MicrosoftAuth from '@/components/templates/authentication/MicrosoftAuth.vue';

@Component({
  components: { MicrosoftAuth, YggdrasilAuthForm, Loading, ProgressBar },
})
export default class InstanceLoading extends Vue {
  @Action('sendMessage') public sendMessage: any;

}
</script>

<style lang="scss" scoped>
.cancel-button {
  outline-color: red;
  border: 1px;
}
.authentication {
  width: 100%;
  height: 100%;

  display: flex;
  justify-content: center;
  align-items: center;

  position: fixed;
  top: 0;
  left: 0;
  background-color: rgba(#1c1c1c, 0.82);
  backdrop-filter: blur(2px);
  z-index: 1000;

  .body-contents {
    background-color: #313131;
    border-radius: 10px;
    box-shadow: 0 6.6px 22.66px 0 #00000040;
    padding: 3rem;
    width: 590px;
    position: relative;
    z-index: 1;

    .back {
      position: absolute;
      display: flex;
      align-items: center;
      top: 1rem;
      left: 1rem;
      padding: 1rem;
      cursor: pointer;
      transition: background-color 0.25s ease-in-out, opacity 0.25s ease-in-out, max-width 0.25s ease-in-out;
      border-radius: 5px;
      opacity: 0.5;
      max-width: 45px;
      overflow: hidden;

      &:hover {
        opacity: 1;
        background-color: black;
        max-width: 180px;

        span {
          opacity: 1;
        }
      }

      span {
        white-space: nowrap;
        display: inline-block;
        margin-left: 1.5rem;
        opacity: 0;
        transition: opacity 0.15s ease-in-out;
      }
    }

    .button {
      height: 100px;
      background-color: #454545;
      width: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 5px;
      box-shadow: 0 0 0 0 #00000040;
      transition: background-color 0.2s ease-in-out, transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;

      &:focus {
        outline: none;
      }

      &:hover {
        background-color: #575757;
        transform: translateX(0.3rem) translateY(-0.3rem);
        box-shadow: -0.3rem 0.3rem 5px 0 #00000040;
      }
    }

    .or {
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 1.5rem 0;
      position: relative;

      &::after {
        background-color: rgba(white, 0.2);
        content: '';
        position: absolute;
        top: 50%;
        left: 0;
        transform: translatey(-50%);
        height: 2px;
        border-radius: 4px;
        width: 100%;
        z-index: -1;
      }

      span {
        background-color: #313131;
        padding: 0 3rem;
        text-transform: uppercase;
        font-weight: bold;
        color: rgba(white, 0.25);
      }
    }

    .checks {
      svg {
        color: var(--color-primary-button);
        transform: translateY(-5px) scale(0.9);
      }

      .fa-check {
        animation: fadeInFadeOut 3.5s ease-in-out infinite;
      }

      .two {
        animation-delay: 0.5s;
      }

      .three {
        animation-delay: 1s;
      }

      .four {
        animation-delay: 1.5s;
      }

      @keyframes fadeInFadeOut {
        0%,
        100% {
          color: var(--color-primary-button);
          transform: translateY(-5px) scale(0.9);
        }
        50% {
          color: white;
          transform: translateY(20px) scale(1);
        }
      }
    }
  }
}
</style>
