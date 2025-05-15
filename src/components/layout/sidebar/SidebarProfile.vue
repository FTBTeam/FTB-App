<script lang="ts" setup>
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import {createLogger} from '@/core/logger';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import Popover from '@/components/ui/Popover.vue';
import {ref, useTemplateRef} from 'vue';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { AuthProfile } from '@/core/types/appTypes.ts';
import { faEdit, faPlus, faQuestion, faTimes, faTrash } from '@fortawesome/free-solid-svg-icons';
import {UiButton} from "@/components/ui";
import {useAttachDomEvent} from "@/composables";

const accountsStore = useAccountsStore();

const { disabled = false } = defineProps<{
  disabled?: boolean
}>()

const editMode = ref(false);
const loading = ref(false);
const open = ref(false);

const sidebarRef = useTemplateRef<HTMLDivElement>("sidebarRef");

useAttachDomEvent<MouseEvent>('click', event => {
  if (sidebarRef.value?.contains(event.target as Node)) {
    return;
  }

  open.value = false;
});

const logger = createLogger("SidebarProfile.vue")

async function removeProfile(profile: AuthProfile) {
  loading.value = true;

  try {
    logger.debug(`Removing profile ${profile.uuid}`)
    const data = await sendMessage("profiles.remove", {
      uuid: profile.uuid
    })

    if (data.success) {
      await accountsStore.loadProfiles()
    } else {
      logger.debug('Failed to remove profile');
    }
  } catch (error) {
    logger.debug('Failed to remove profile due to message errors', error);
  }

  loading.value = false;
}

async function setActiveProfile(profile: AuthProfile) {
  loading.value = true;
  try {
    logger.debug(`Setting active profile ${profile.uuid}`)
    const data = await sendMessage("profiles.setActiveProfile", {
      uuid: profile.uuid
    })

    if (data.success) {
      await accountsStore.loadProfiles()
    } else {
      logger.debug('Failed to set active profile');
    }
  } catch (error) {
    logger.debug('Failed to set active profile due to message errors', error);
  }

  loading.value = false;
}

function openSignIn() {
  accountsStore.openSignIn(true);
  open.value = false;
}

function openSignInFtb() {
  accountsStore.openSignInFtb(true);
  open.value = false;
}
</script>

<template>
  <div class="profile-area" :class="{ disabled }" ref="sidebarRef">
    <div class="profile" v-if="(accountsStore.mcProfiles && accountsStore.mcProfiles.length)">
      <div class="avatar cursor-pointer" @click="() => open = !open">
        <img
          :src="getMinecraftHead(accountsStore.mcActiveProfile?.uuid ?? null)"
          alt="Profile"
          class="rounded"
          width="35"
          height="35"
        />
      </div>

      <div class="profile-switch" :class="{open}" v-show="!disabled">
        <section class="mb-8">
          <div class="headings">
            <div class="main">
              FTB Account
            </div>
            <FontAwesomeIcon
              class="cursor-pointer"
              v-if="accountsStore.mcProfiles.length"
              :icon="editMode ? faTimes : faEdit"
              @click="editMode = !editMode"
            />
          </div>

          <div class="accounts">
            <div v-if="accountsStore.ftbAccount" class="account">
              <div v-if="accountsStore.ftbAccount.accountData.picture">
                <div class="avatar">
                  <img :src="accountsStore.ftbAccount.accountData.picture" alt="Profile" class="rounded-lg w-[40px] h-[40px] aspect-square" />
                </div>
              </div>

              <div>
                <p class="font-bold">{{ accountsStore.ftbAccount.accountData.preferred_username ?? accountsStore.ftbAccount.accountData.given_name ?? "Unknown?" }}</p>
                <p class="text-sm" v-if="accountsStore.isPatreon">Patreon Member</p>
              </div>
            </div>
          </div>

          <UiButton v-if="!accountsStore.ftbAccount" size="small" type="primary" :icon="faPlus" @click="() => openSignInFtb()">Add FTB Account</UiButton>
        </section>
        
        <section>
          <div class="headings">
            <div class="main">
              Accounts
            </div>
            <FontAwesomeIcon
              class="cursor-pointer"
              v-if="accountsStore.mcProfiles.length"
              :icon="editMode ? faTimes : faEdit"
              @click="editMode = !editMode"
            />
          </div>
          
          <div class="accounts" v-if="accountsStore.mcProfiles && accountsStore.mcProfiles.length">
            <div
              class="account"
              :class="{ loading, active: accountsStore.mcActiveProfile?.uuid === item.uuid }"
              v-for="(item, key) in accountsStore.mcProfiles"
              :key="key"
              @click="() => setActiveProfile(item)"
            >
              <div class="avatar">
                <img :src="getMinecraftHead(item.uuid)" alt="Profile" class="rounded" />
              </div>
              <div class="name selectable">
                <div class="username-container" :title="`${item.username} - Click to set as active profile`">
                  <div class="username">{{ item.username }}</div>
                  <span class="opacity-50" v-if="accountsStore.mcActiveProfile?.uuid === item.uuid">(active)</span>
                </div>
                <div
                  class="trash bg-red-500 hover:bg-red-600 transition-colors"
                  :class="{ active: editMode }"
                  @click.stop="() => removeProfile(item)"
                >
                  <FontAwesomeIcon :icon="faTrash" />
                </div>
              </div>
            </div>
          </div>
          
          <UiButton size="small" type="primary" :icon="faPlus" @click="() => openSignIn()">Add Minecraft Account</UiButton>
        </section>
      </div>
    </div>
    <Popover text="Sign in to your Minecraft account" v-else>
      <div class="profile-placeholder" @click="() => openSignIn()">
        <div class="fake-avatar">
          <FontAwesomeIcon :icon="faQuestion" />
        </div>
      </div>
    </Popover>
  </div>
</template>

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

  .profile-switch {
    position: absolute;
    left: 100%;
    top: 0;
    width: 320px;
    height: calc(100% - 1px);
    z-index: 1000;
    opacity: 0;
    background-color: #363636;
    border-left: 1px solid rgba(white, .1);
    border-right: 1px solid rgba(white, .1);
    padding: 1rem;

    transition: transform 0.2s ease-in-out, opacity 0.2s ease-in-out, visibility 0.2s ease-in-out;
    transform: translateX(1rem);
    visibility: hidden;
    
    &.open {
      visibility: visible;
      transform: translateX(0);
      opacity: 1;
    }

    section {
      position: relative;
    }

    .headings {
      display: flex;
      align-items: center;
      margin-bottom: 1.2rem;

      .main {
        display: flex;
        align-items: center;
        flex: 1;
        font-weight: bold;
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
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .account {
      display: flex;
      align-items: center;
      position: relative;
      transition: background-color 0.2s ease-in-out;
      border-radius: 5px;
      cursor: pointer;
      padding: .5rem;
      border: 1px solid rgba(white, 0.1);

      &.loading {
        cursor: not-allowed;
        *,
        & {
          pointer-events: none;
        }
      }

      &.active {
        background-color: rgba(white, 0.1);
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
