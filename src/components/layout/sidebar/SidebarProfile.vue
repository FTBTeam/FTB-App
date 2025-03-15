<template>
  <div class="profile-area" :class="{ disable }">
    <div class="profile" v-if="(getProfiles && getProfiles.length)">
      <div class="avatar">
        <img
          :src="getMinecraftHead(getActiveProfile ? getActiveProfile.uuid : null)"
          alt="Profile"
          class="rounded"
          width="35"
          height="35"
        />
      </div>

      <div class="profile-switch" v-show="!disable">
        <section>
          <div class="headings">
            <div class="main">
              <img src="@/assets/images/minecraft.webp" alt="Minecraft grass block" />
              Accounts
            </div>
            <font-awesome-icon
              class="cursor-pointer"
              v-if="getProfiles.length"
              :icon="editMode ? 'times' : 'edit'"
              @click="editMode = !editMode"
            />
          </div>
          <div class="accounts" v-if="getProfiles && getProfiles.length">
            <div
              class="account hoverable"
              :class="{ loading, active: getActiveProfile.uuid === item.uuid }"
              v-for="(item, key) in getProfiles"
              :key="key"
              @click="() => setActiveProfile(item)"
            >
              <div class="avatar">
                <img :src="getMinecraftHead(item.uuid)" alt="Profile" class="rounded" />
              </div>
              <div class="name selectable">
                <div class="username-container" :title="`${item.username} - Click to set as active profile`">
                  <div class="username">{{ item.username }}</div>
                  <span class="opacity-50" v-if="getActiveProfile.uuid === item.uuid">(active)</span>
                </div>
                <div
                  class="trash bg-red-500 hover:bg-red-600 transition-colors"
                  :class="{ active: editMode }"
                  @click.stop="() => removeProfile(item)"
                >
                  <font-awesome-icon icon="trash" />
                </div>
              </div>
            </div>
          </div>

          <div class="add-new" @click="openSignIn()">
            <div class="add-button px-4 py-2"><font-awesome-icon icon="plus" /> Add account</div>
          </div>
        </section>
      </div>
    </div>
    <popover text="Sign in to your Minecraft account" v-else>
      <div class="profile-placeholder" @click="$emit('signin')">
        <div class="fake-avatar">
          <font-awesome-icon icon="question" />
        </div>
      </div>
    </popover>
  </div>
</template>

<script lang="ts">
import {AuthProfile} from '@/modules/core/core.types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import {createLogger} from '@/core/logger';

@Component({
  methods: {getMinecraftHead}
})
export default class SidebarProfile extends Vue {
  @Prop({ default: false }) disable!: boolean;

  @Getter('getProfiles', { namespace: 'core' }) getProfiles!: AuthProfile[];
  @Getter('getActiveProfile', { namespace: 'core' }) getActiveProfile!: AuthProfile;
  
  @Action('openSignIn', { namespace: 'core' }) openSignIn!: any;
  @Action('loadProfiles', { namespace: 'core' }) loadProfiles: any;

  private logger = createLogger("SidebarProfile.vue")
  
  editMode = false;
  loading = false;

  async removeProfile(profile: AuthProfile) {
    this.loading = true;

    try {
      this.logger.debug(`Removing profile ${profile.uuid}`)
      const data = await sendMessage("profiles.remove", {
        uuid: profile.uuid
      })

      if (data.success) {
        this.loadProfiles();
      } else {
        this.logger.debug('Failed to remove profile');
      }
    } catch (error) {
      this.logger.debug('Failed to remove profile due to message errors', error);
    }

    this.loading = false;
  }

  async setActiveProfile(profile: AuthProfile) {
    this.loading = true;
    try {
      this.logger.debug(`Setting active profile ${profile.uuid}`)
      const data = await sendMessage("profiles.setActiveProfile", {
        uuid: profile.uuid
      })

      if (data.success) {
        this.loadProfiles();
      } else {
        this.logger.debug('Failed to set active profile');
      }
    } catch (error) {
      this.logger.debug('Failed to set active profile due to message errors', error);
    }

    this.loading = false;
  }
}
</script>

<style scoped lang="scss">
.selectable {
  user-select: text;
}

.profile-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem 0;
  cursor: pointer;

  &:hover .fake-avatar svg {
    transform: rotateZ(180deg);
  }

  .fake-avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background-color: #161313;
    border: 2px solid rgba(white, 0.3);

    svg {
      transition: transform 0.25s ease-in-out;
    }
  }
}

.profile-area {
  position: relative;
  margin-top: 0.5rem;

  &.disable {
    opacity: 0.5;
  }

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
      transition-delay: 0.15s;
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
        transform: translateY(50%);
        transform-origin: left bottom;
        box-shadow: 0 0 1.5rem rgba(0, 0, 0, 0);
        opacity: 0;

        transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out, opacity 0.2s ease-in-out;
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

      .main {
        display: flex;
        align-items: center;
        flex: 1;
      }

      .fa-edit {
        opacity: 0.5;
        cursor: pointer;
      }

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
      margin-bottom: 1rem;
      position: relative;
      z-index: 1;
      max-height: 280px;
    }

    .account {
      display: flex;
      align-items: center;
      position: relative;
      transition: background-color 0.2s ease-in-out;

      &.loading {
        cursor: not-allowed;
        *,
        & {
          pointer-events: none;
        }
      }

      &.active {
        background-color: black;

        .ms-identifier {
          background-color: black;
        }
      }

      &.hoverable {
        padding: 0.8rem 1rem;
        border-radius: 5px;
        &:not(:last-child) {
          margin-bottom: 0.5rem;
        }
        cursor: pointer;

        &:hover {
          background-color: var(--color-primary-button);

          .ms-identifier {
            background-color: black;
          }
        }
      }

      .avatar {
        position: relative;
        margin-right: 1.2rem;
        width: 30px;
        height: 30px;
        img {
          width: 30px;
          height: 30px;
        }

        .ms-identifier {
          transition: background-color 0.2s ease-in-out;
          position: absolute;
          padding: 0.3rem;
          background-color: #161313;
          border-radius: 5px;
          left: -35%;
          bottom: -35%;

          img {
            width: 12px;
            height: 12px;
          }
        }
      }

      .name {
        flex: 1;
        display: flex;
        align-items: center;

        .username-container {
          flex: 1;
          span {
            display: block;
            margin-top: -0.25rem;
            font-size: 0.75rem;
          }
        }

        .username {
          max-width: 250px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          padding-right: 1rem;
        }

        .trash {
          padding: 0.15rem 0.5rem;
          border-radius: 5px;
          opacity: 0;
          visibility: hidden;
          transition: opacity 0.2s ease-in-out, visibility 0.2s ease-in-out;

          &.active {
            opacity: 1;
            visibility: visible;
          }
        }

        img {
          height: 1em;
          width: 1em;
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
