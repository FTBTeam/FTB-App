<template>
  <div class="global__fullscreen-modal authentication" @mousedown.self="() => close()">
    <div class="body-contents text-center">
      <div class="back" v-if="!onMainView && !loggedIn && !loading" @click="back()">
        <font-awesome-icon icon="chevron-left" />
        <span>Back to options</span>
      </div>
      <div class="main" v-if="loading">
        <Loading />
      </div>

      <div class="main text-center" v-else-if="error.length > 0">
        <h2 class="text-3xl font-bold">Something went wrong.</h2>
        <p class="text-red-400 font-bold mt-6 mb-8">{{ error }}</p>

        <ftb-button color="primary" class="px-6 py-4" @click="() => close()">Close</ftb-button>
      </div>

      <div class="main" v-else-if="onMainView && !loggedIn">
        <h3 class="text-2xl mb-4"><b>Minecraft Login</b></h3>
        <p class="mb-8">
          Now that Minecraft uses Microsoft to login, we now require to login to your Microsoft account or your Mojang
          account.
        </p>

        <h4 class="text-center font-bold mb-8">Sign in with</h4>

        <button
          class="actionable-button"
          @click="
            () => {
              onMsAuth = true;
              onMcAuth = false;
              onMainView = false;
            }
          "
        >
          <img src="@/assets/images/branding/microsoft.svg" alt="Microsoft Login" />
        </button>

        <template v-if="mojangAuthAllowed">
          <div class="or">
            <span>or</span>
          </div>

          <button
            class="actionable-button"
            @click="
              () => {
                onMcAuth = true;
                onMsAuth = false;
                onMainView = false;
              }
            "
          >
            <img src="@/assets/images/branding/mojang.svg" alt="Mojang Login" />
          </button>

          <small class="text-red-400 mt-4 block text-center"
            >Mojang accounts must be migrated before March 10th, see this
            <a
              class="text-gray-400 hover:text-white"
              href="https://www.minecraft.net/en-us/article/last-call-voluntarily-migrate-java-accounts"
              @click.prevent="openExternal"
              >official blog</a
            >
            post for more information</small
          >
        </template>
      </div>

      <div class="logged-in text-center" v-else-if="loggedIn">
        <h2 class="mb-4 text-3xl font-bold">You're set!</h2>
        <p>You can add & remove profiles by using the profile switcher in the bottom left of the App.</p>
        <div class="checks">
          <font-awesome-icon icon="check" class="my-20 one" size="5x" />
          <font-awesome-icon icon="check" class="my-20 two" size="5x" />
          <font-awesome-icon icon="check" class="my-20 three" size="5x" />
          <font-awesome-icon icon="check" class="my-20 four" size="5x" />
        </div>
        <ftb-button color="primary" class="px-6 py-4" @click="() => close()">Finish</ftb-button>
      </div>

      <div class="auth-views" v-else>
        <microsoft-auth v-if="onMsAuth" @authenticated="authenticated()" @error="(e) => (error = e)" />
        <yggdrasil-auth-form v-if="onMcAuth" :uuid="uuid" @authenticated="authenticated()" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import Loading from '@/components/atoms/Loading.vue';
import { Action } from 'vuex-class';
import MicrosoftAuth from '@/components/templates/authentication/MicrosoftAuth.vue';
import { Prop } from 'vue-property-decorator';
import dayjs from 'dayjs';
import { RouterNames } from '@/router';

@Component({
  components: { MicrosoftAuth, YggdrasilAuthForm, Loading },
})
export default class Authentication extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('loadProfiles', { namespace: 'core' }) public loadProfiles: any;

  @Prop() public jump!: string;
  @Prop() public uuid!: string;
  @Prop() public tryAgainInstanceUuid!: any;

  onMainView = true;
  loggedIn = false;
  error = '';
  loading = false;

  onMcAuth = false;
  onMsAuth = false;

  public mounted() {
    if (this.jump === 'mc') {
      this.onMainView = false;
      this.onMcAuth = true;
      this.onMsAuth = false;
    }
  }

  back() {
    this.onMainView = true;
    this.onMsAuth = false;
    this.onMcAuth = false;
    this.error = '';
  }

  authenticated() {
    this.loggedIn = true;
    this.back();
  }

  close() {
    if (this.tryAgainInstanceUuid && this.loggedIn) {
      this.$router.push({ name: RouterNames.ROOT_LAUNCH_PACK, query: { uuid: this.tryAgainInstanceUuid } });
    }

    this.$emit('close');
  }

  get mojangAuthAllowed() {
    return !dayjs().isAfter('2022-03-10');
  }
}
</script>

<style lang="scss" scoped>
.authentication {
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
</style>
