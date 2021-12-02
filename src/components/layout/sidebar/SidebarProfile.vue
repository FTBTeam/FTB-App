<template>
  <div class="profile-area">
    <div class="profile" v-if="getActiveProfile">
      <div class="avatar">
        <img
          :src="`https://api.mymcuu.id/head/${getActiveProfile.uuid}`"
          alt="Profile"
          class="rounded"
          width="35"
          height="35"
        />
      </div>

      <div class="profile-switch">
        <section>
          <div class="headings"><img src="@/assets/images/minecraft.webp" alt="Minecraft grass block" />Accounts</div>
          <div class="accounts" v-if="getProfiles.length">
            <div class="account" v-for="(item, key) in getProfiles" :key="key">
              <div class="avatar">
                <img :src="`https://api.mymcuu.id/head/${item.uuid}`" alt="Profile" class="rounded" />
                <div class="ms-identifier" v-if="item.type === 'microsoft'">
                  <img src="@/assets/images/branding/microsoft-squares.png" alt="Microsoft account" />
                </div>
              </div>
              <div class="name">
                {{ item.username }}
              </div>
            </div>
          </div>

          <div class="add-new">
            <div class="add-button px-4 py-2"><font-awesome-icon icon="plus" /> Add account</div>
          </div>
        </section>

        <section>
          <div class="mt-area">
            <div class="headings"><img src="@/assets/mtg-tiny-desat.png" alt="MineTogether Logo" />MineTogether</div>
            <div class="account" v-if="auth.token">
              <div class="avatar">
                <img :src="`https://api.mymcuu.id/head/${avatarName}`" alt="Profile" class="rounded" />
              </div>
              <div class="meta">
                <div class="name">
                  {{ auth.token.mc.display !== null ? auth.token.mc.display : auth.token.username }}
                </div>
                <div class="hash">{{ auth.token.mc.friendCode }}</div>
              </div>
            </div>
            <div class="add inline-block" v-else>
              <router-link :to="{ name: 'integrations' }">
                <div class="add-button px-4 py-2"><font-awesome-icon icon="sign-in-alt" /> Sign-in to MineTogether</div>
              </router-link>
            </div>
          </div>
        </section>
      </div>
    </div>
    <div class="profile-placeholder" v-else>
      <div class="fake-avatar"></div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Getter, State } from 'vuex-class';
import { AuthProfile } from '@/modules/core/core.types';
import { AuthState } from '@/modules/auth/types';

@Component
export default class SidebarProfile extends Vue {
  @Getter('getProfiles', { namespace: 'core' }) private getProfiles!: AuthProfile[];
  @Getter('getActiveProfile', { namespace: 'core' }) private getActiveProfile!: AuthProfile;

  @State('auth') private auth!: AuthState;

  mounted() {
    console.log(this.auth);
  }

  get avatarName() {
    const provider = this.auth.token?.accounts.find(s => s.identityProvider === 'mcauth');
    return provider !== undefined && provider !== null ? provider.userId : 'MHF_Steve';
  }
}
</script>

<style scoped lang="scss">
.profile-area {
  position: relative;
  margin-bottom: 0.5rem;
  margin-top: 0.5rem;

  .profile {
    display: flex;
    align-items: center;
    padding: 1rem 0;

    > .avatar {
      margin: 0 auto;
    }
  }

  img {
    -webkit-user-drag: none;
  }

  &:hover {
    .profile-switch {
      transform: scale(1);
      opacity: 1;
    }

    .profile-switch section:not(:last-child) {
      transform: translateY(0);
      box-shadow: 0 0 1.5rem rgba(0, 0, 0, 0.3);
      opacity: 1;
    }
  }

  .profile-switch {
    position: absolute;
    left: calc(100% - 1rem);
    bottom: 1rem;
    min-width: 300px;
    padding-left: 2rem;
    z-index: 1000;
    transform: scale(0);
    opacity: 0;
    transform-origin: 2rem 100%;

    transition: transform 0.2s ease-in-out, opacity 0.2s ease-in-out;

    section {
      padding: 1.2rem;
      background-color: #161313;
      border-radius: 5px;
      box-shadow: 0 0 1.5rem rgba(0, 0, 0, 0.3);
      position: relative;

      &:not(:last-child) {
        margin-bottom: 0.3rem;
        transform: translateY(90%);
        box-shadow: 0 0 1.5rem rgba(0, 0, 0, 0);
        opacity: 0;

        transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out, opacity 0.3s ease-in-out;
        transition-delay: 0.16s;
      }
    }

    &::after {
      content: '';
      position: absolute;
      left: 2rem;
      bottom: 0;
      width: 0;
      height: 0;
      border-right: 1rem solid transparent;
      border-bottom: 1rem solid transparent;
      border-top: 1rem solid #161313;
      transform: rotateZ(-45deg);
    }

    .headings {
      display: flex;
      align-items: center;
      color: rgba(white, 0.9);
      margin-bottom: 1.2rem;

      img {
        margin-right: 0.5rem;
        height: 1em;
      }
    }

    .add-button {
      display: inline-block;
      border: 1px solid rgba(white, 0.3);
      font-size: 0.875rem;
      cursor: pointer;
      transition: background-color 0.2s ease-in-out;

      &:hover {
        background-color: rgba(white, 0.1);
      }

      svg {
        margin-right: 0.5rem;
      }
    }

    .add-new {
      display: flex;
      align-items: center;
    }

    .accounts {
      margin-bottom: 2rem;
    }

    .account {
      display: flex;
      align-items: center;

      &:not(:last-child) {
        margin-bottom: 1rem;
      }

      .avatar {
        position: relative;
        margin-right: 1.5rem;
        img {
          width: 30px;
          height: 30px;
        }

        .ms-identifier {
          position: absolute;
          padding: 0.3rem;
          background-color: #161313;
          border-radius: 5px;
          right: -40%;
          bottom: -40%;

          img {
            width: 15px;
            height: 15px;
          }
        }
      }

      .name {
        display: flex;
        align-items: center;

        img {
          height: 1em;
          margin-right: 0.5rem;
        }
      }

      .meta {
        .hash {
          font-size: 0.8rem;
          color: rgba(white, 0.8);
        }
      }
    }
  }
}
</style>
