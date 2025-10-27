<script lang="ts" setup>
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import {createLogger} from '@/core/logger';
import {ref, useTemplateRef} from 'vue';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { AuthProfile } from '@/core/types/appTypes.ts';
import {faExternalLink, faPlus} from '@fortawesome/free-solid-svg-icons';
import {Popover, UiButton} from "@/components/ui";
import {useAttachDomEvent} from "@/composables";
import SidebarProfileItem from "@/components/layout/sidebar/SidebarProfileItem.vue";
import {dialogsController} from "@/core/controllers/dialogsController.ts";

const accountsStore = useAccountsStore();

const { disabled = false } = defineProps<{
  disabled?: boolean
}>()

const loading = ref(false);
const open = ref(false);
const awaitingConfirm = ref(false);
const specialOpen = ref(false);

const sidebarRef = useTemplateRef<HTMLDivElement>("sidebarRef");

useAttachDomEvent<MouseEvent>('click', event => {
  // Don't close because a dialog was clicked
  if (awaitingConfirm.value) {
    return;
  }
  
  if (sidebarRef.value?.contains(event.target as Node)) {
    return;
  }

  closeMenu()
});

const logger = createLogger("SidebarProfile.vue")

async function removeProfile(profile: AuthProfile) {
  if (!(await confirm())) {
    return;
  }
  
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
  closeMenu()
}

function openSignInFtb() {
  accountsStore.openSignInFtb(true);
  closeMenu()
}

async function ftbSignOut() {
  if (await confirm()) {
    await accountsStore.signOutFtb();
  }
}

async function confirm() {
  awaitingConfirm.value = true;
  const result = await dialogsController.createConfirmationDialog("Are you sure?", "Profiles can be added back at any point but you will need to go through the login steps again.");
  setTimeout(() => awaitingConfirm.value = false, 200)
  return result
}

function openMenu(event: MouseEvent) {
  if (open.value) {
    closeMenu()
    return;
  }
  
  open.value = true;
  if (event.shiftKey) {
    specialOpen.value = true;
  }
}

function closeMenu() {
  open.value = false;
  specialOpen.value = false;
}
</script>

<template>
  <div class="profile-area" :class="{ disabled }" ref="sidebarRef">
    <div class="profile">
      <div class="avatar cursor-pointer" @click="openMenu($event)">
        <Popover :text="!open ? 'Sign in or manage your accounts' : undefined">
            <img
              :src="getMinecraftHead(accountsStore.mcActiveProfile?.uuid ?? null)"
              alt="Profile"
              class="rounded"
              width="35"
              height="35"
            />
        </Popover>
      </div>

      <div class="profile-switch" :class="{open}" v-show="!disabled">
        <section class="mb-8">
          <p class="font-bold mb-4">Minecraft Accounts</p>
          
          <div class="accounts" v-if="accountsStore.mcProfiles && accountsStore.mcProfiles.length">
            <SidebarProfileItem
              v-for="(item, key) in accountsStore.mcProfiles"
              :key="key"
              :profile="{
                name: item.username,
                avatarUrl: getMinecraftHead(item.uuid)
              }" 
              :on-delete="() => removeProfile(item)" 
              :on-select="() => setActiveProfile(item)"
              :subtext="accountsStore.mcActiveProfile?.uuid === item.uuid ? 'Active Profile' : undefined"
              :active="accountsStore.mcActiveProfile?.uuid === item.uuid" />
          </div>
          
          <UiButton size="small" type="primary" :icon="faPlus" @click="() => openSignIn()">Add Minecraft Account</UiButton>
        </section>

        <section v-if="specialOpen">
          <p class="font-bold mb-4">FTB Account</p>

          <div class="accounts" v-if="accountsStore.ftbAccount">
            <SidebarProfileItem :profile="{
              name: accountsStore.ftbAccount?.accountData.preferred_username ?? accountsStore.ftbAccount?.accountData.given_name ?? 'Unknown?',
              avatarUrl: accountsStore.ftbAccount?.accountData.picture
            }"
                                :subtext="accountsStore.isPatreon ? '❤️ Patreon Member' : undefined"
                                :active="true"
                                :on-delete="ftbSignOut"
                                :on-select="() => {}" />
            
            <a href="https://account.feed-the-beast.com" target="_blank"><UiButton :icon="faExternalLink">Go to accounts page</UiButton></a>
          </div>

          <UiButton v-if="!accountsStore.ftbAccount" size="small" type="primary" :icon="faPlus" @click="() => openSignInFtb()">Add FTB Account</UiButton>
        </section>
      </div>
    </div>
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
    width: 360px;
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
    
    .accounts {
      margin-bottom: 1rem;
      position: relative;
      z-index: 1;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
  }
}
</style>
