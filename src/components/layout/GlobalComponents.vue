<script lang="ts" setup>
import Changelog from '@/components/groups/changelogs/Changelog.vue';
import Dialogs from '@/components/groups/global/dialog/Dialogs.vue';
import Alerts from '@/components/groups/global/Alerts.vue';
import ContentMenuGlobal from '@/components/groups/global/contextMenu/ContentMenuGlobal.vue';
import LaunchInstanceDialog from '@/components/modals/LaunchInstanceDialog.vue';
import {useAccountsStore} from '@/store/accountsStore.ts';
import ModpackPreview from "@/components/groups/global/modpackPreview/ModpackPreview.vue";
import ImagePreview from "@/components/groups/global/imagePreview/ImagePreview.vue";
import SigninModal from "@/components/groups/auth/SigninModal.vue";

const accountStore = useAccountsStore();
</script>

<template>
  <div class="global-components">
    <ModpackPreview/>
    <content-menu-global/>
    <ImagePreview/>

    <SigninModal :open="accountStore.activeSignInModal !== null" :account-type="accountStore.activeSignInModal ?? 'MICROSOFT'" @closed="() => accountStore.openSignIn(null)"/>
<!--    <MicrosoftLoginModal :open="accountStore.signInOpen" @closed="() => accountStore.openSignIn(false)"/>-->
<!--    <FtbLoginModal :open="accountStore.signInFtbOpen" @closed="() => accountStore.openSignInFtb(false)"/>-->

    <!-- Only checks for an update once during startup -->
    <changelog/>
    <dialogs/>
    <alerts/>
    <launch-instance-dialog/>
  </div>
</template>

<style lang="scss" scoped>
.alerts {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 5000;

  display: flex;
  flex-direction: column;
  align-items: flex-end;

  .alert {
    display: flex;
    align-items: center;
    padding: 0.5rem 1rem;
    margin-top: 0.5rem;
    border-radius: var(--border-radius);

    span {
      margin-right: 0.5rem;
    }

    .message {
      margin-right: 0.5rem;
    }

    .close {
      cursor: pointer;
      padding: 0 0.5rem;
    }
  }
}
</style>
