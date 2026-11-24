<script lang="ts" setup>
import {sendMessage} from '@/core/websockets/websocketsApi';
import {getMinecraftHead} from '@/utils/helpers/mcsHelpers';
import {createLogger} from '@/core/logger';
import {ref, useTemplateRef} from 'vue';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { AuthProfile } from '@/core/types/appTypes.ts';
import { AccountProvider, FtbAccount } from '@/core/types/javaApi';
import {faArrowsLeftRight, faExternalLink, faPlus, faRefresh, faTrash} from '@fortawesome/free-solid-svg-icons';
import {MenuOptions, openContextMenu} from '@/core/context/contextMenu.ts';
import {Popover} from "@/components/ui";
import appPlatform from '@platform';
import {useAttachDomEvent} from "@/composables";
import {dialogsController} from "@/core/controllers/dialogsController.ts";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ProfileSelectorItem from "@/components/layout/sidebar/ProfileSelectorItem.vue";

const accountsStore = useAccountsStore();

const { disabled = false } = defineProps<{
  disabled?: boolean
}>()

const loading = ref(false);
const open = ref(false);
const awaitingConfirm = ref(false);

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
      provider: "MICROSOFT",
      id: profile.uuid
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
      provider: "MICROSOFT",
      id: profile.uuid
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

async function removeFtbAccount(account: FtbAccount) {
  if (!(await confirm())) {
    return;
  }

  loading.value = true;
  try {
    if (!(await accountsStore.removeFtbAccount(account.id))) {
      logger.debug('Failed to remove FTB account');
    }
  } catch (error) {
    logger.debug('Failed to remove FTB account due to message errors', error);
  }
  loading.value = false;
}

async function setActiveFtbAccount(account: FtbAccount) {
  if (account.id === accountsStore.ftbAccount?.id) {
    return;
  }

  loading.value = true;
  try {
    if (!(await accountsStore.setActiveFtbAccount(account.id))) {
      logger.debug('Failed to set active FTB account');
    }
  } catch (error) {
    logger.debug('Failed to set active FTB account due to message errors', error);
  }
  loading.value = false;
}

const signedOutText = 'Signed out, click to sign in again';

function minecraftMenu(profile: AuthProfile): MenuOptions {
  const active = profile.uuid === accountsStore.mcActiveProfile?.uuid;
  return [
    {title: 'Switch to this account', icon: faArrowsLeftRight, hidden: active || profile.notLoggedIn, action: () => setActiveProfile(profile)},
    {title: 'Sign in again', icon: faRefresh, hidden: !profile.notLoggedIn, action: () => signInAgain('MICROSOFT', profile.uuid)},
    {title: 'Remove account', icon: faTrash, color: 'danger', action: () => removeProfile(profile)},
  ];
}

function ftbMenu(account: FtbAccount): MenuOptions {
  const active = account.id === accountsStore.ftbAccount?.id;
  return [
    {title: 'Switch to this account', icon: faArrowsLeftRight, hidden: active || account.notLoggedIn, action: () => setActiveFtbAccount(account)},
    {title: 'Sign in again', icon: faRefresh, hidden: !account.notLoggedIn, action: () => signInAgain('FTB', account.id)},
    {title: 'Go to accounts page', icon: faExternalLink, action: () => appPlatform.utils.openUrl('https://account.feed-the-beast.com')},
    {separator: true},
    {title: 'Remove account', icon: faTrash, color: 'danger', action: () => removeFtbAccount(account)},
  ];
}

function signInAgain(provider: AccountProvider, accountId: string) {
  closeMenu();
  accountsStore.openSignIn(provider, accountId);
}

function ftbSubtext(account: FtbAccount) {
  if (account.notLoggedIn) {
    return signedOutText;
  }

  return account.premium ? '❤️ Premium' : undefined;
}

async function confirm() {
  awaitingConfirm.value = true;
  const result = await dialogsController.createConfirmationDialog("Are you sure?", "Profiles can be added back at any point but you will need to go through the login steps again.");
  setTimeout(() => awaitingConfirm.value = false, 200)
  return result
}

function openMenu() {
  if (open.value) {
    closeMenu()
    return;
  }
  
  open.value = true;
}

function closeMenu() {
  open.value = false;
}
</script>

<template>
  <div class="profile-area" :class="{ disabled }" ref="sidebarRef">
    <div class="profile">
      <div class="avatar cursor-pointer border-4 rounded transition-all duration-200 hover:border-white/10" :class="{'border-white/10': open, 'border-transparent': !open}" @click="openMenu()">
        <Popover :text="!open ? 'Sign in or manage your accounts' : undefined">
            <img
              :src="getMinecraftHead(accountsStore.mcActiveProfile?.uuid ?? null)"
              alt="Profile"
              class="rounded-xs"
              width="35"
              height="35"
            />
        </Popover>
      </div>

      <div class="profile-switch" :class="{open}" v-show="!disabled">
        <section class="p-4 border-b border-white/10" v-if="accountsStore.mcActiveProfile">
          <p class="font-bold text-white/80 text-lg mb-4">Playing as</p>

          <ProfileSelectorItem
            :name="accountsStore.mcActiveProfile.username" 
            :avatar="getMinecraftHead(accountsStore.mcActiveProfile.uuid)"
            @options="(elm) => openContextMenu(elm, minecraftMenu(accountsStore.mcActiveProfile!))"
            is-larger 
          >
            <span :class="{'text-red-300': accountsStore.mcActiveProfile.notLoggedIn, 'text-green-300': !accountsStore.mcActiveProfile.notLoggedIn}">{{accountsStore.mcActiveProfile.notLoggedIn ? signedOutText : 'Ready to play'}}</span>
          </ProfileSelectorItem>
        </section>
        
        <section class="p-2">
          <div class="font-bold text-white/80 text-lg px-2 mt-2" v-if="accountsStore.mcActiveProfile">Switch to</div>
          
          <div class="flex flex-col gap-2 max-h-[250px] overflow-y-auto" v-if="accountsStore.mcProfiles && accountsStore.mcProfiles.length">
            <div v-for="(item, key) in accountsStore.mcProfiles">
              <ProfileSelectorItem
                v-if="item.uuid !== accountsStore.mcActiveProfile?.uuid"
                class="px-2 py-2 hover:bg-white/10 rounded-lg cursor-pointer"
                :key="key"
                :name="`${item.username}${item.notLoggedIn ? ` (${signedOutText})` : ''}`"
                :avatar="getMinecraftHead(item.uuid)"
                @options="(elm) => openContextMenu(elm, minecraftMenu(item))"
                @click="() => item.notLoggedIn ? signInAgain('MICROSOFT', item.uuid) : setActiveProfile(item)"
              />
            </div>
            
            <div class="flex gap-4 items-center hover:bg-white/10 p-2 rounded-lg cursor-pointer" @click="() => accountsStore.openSignIn('MICROSOFT')">
              <div class="w-10 h-10 border border-dashed border-blue-500 rounded-lg flex items-center justify-center">
                <FontAwesomeIcon :icon="faPlus" class="text-blue-200" fixed-width />
              </div>
              <p class="text-blue-300">Add Minecraft Account</p>
            </div>
          </div>          
        </section>

        <section class="p-2 pt-4 bg-black/20 border-t border-b border-white/10">
          <p class="font-bold text-white/80 text-lg px-2 mb-2">FTB Accounts</p>

          <div class="flex flex-col gap-2 max-h-[250px] overflow-y-auto">
            <ProfileSelectorItem 
              v-for="account in accountsStore.ftbAccounts" :key="account.id"
              is-rounded
              :class="{'px-2 py-2 hover:bg-white/10 rounded-lg cursor-pointer border border-transparent': true, 'bg-blue-600/30 border-blue-400!': account.id === accountsStore.ftbAccount?.id}"
              :name="account.username"
              :avatar="account.avatarUrl"
              @options="e => openContextMenu(e, ftbMenu(account))"
              @click="() => account.notLoggedIn ? signInAgain('FTB', account.id) : setActiveFtbAccount(account)">
              <div class="flex flex-wrap gap-2">
                <span v-if="account.id === accountsStore.ftbAccount?.id">Active</span>
                <span v-if="account.id === accountsStore.ftbAccount?.id">·</span>
                {{ftbSubtext(account)}}
              </div>
            </ProfileSelectorItem>

            <div class="flex gap-4 items-center hover:bg-white/10 p-2 rounded-lg cursor-pointer" @click="() => accountsStore.openSignIn('FTB')">
              <div class="w-10 h-10 border border-dashed border-blue-500 rounded-full flex items-center justify-center">
                <FontAwesomeIcon :icon="faPlus" class="text-blue-200" fixed-width />
              </div>
              <p class="text-blue-300">Add FTB Account</p>
            </div>
          </div>          
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
    border-radius: var(--border-radius);
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
    left: 120%;
    bottom: 1rem;
    width: 320px;
    max-height: calc(100% - 24px);
    overflow-y: auto;
    z-index: 1000;
    opacity: 0;
    background-color: #363636;
    border: 1px solid rgba(white, .1);
    border-radius: var(--border-radius);
    
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
  }
}
</style>
