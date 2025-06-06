import {alertController} from '@/core/controllers/alertController';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { AuthProfile } from '@/core/types/appTypes.ts';

/**
 * Selects the active profile or attempts to resolve the profile by UUID
 * If no profile is found, an error is shown and null is returned
 * 
 * @param profileUuid The UUID of the profile to select
 */
export function getProfileOrDefaultToActive(profileUuid: string | null = null): AuthProfile | null {
  const accountStore = useAccountsStore();
  
  let profile: AuthProfile | null = null;
  if (profileUuid === null) {
    const activeProfile = accountStore.mcActiveProfile;
    if (!activeProfile) {
      alertController.error("No active profile selected, please select a profile or login to continue");
      // Tell the app to open the login window
      accountStore.openSignIn()
      return null;
    }

    profile = activeProfile;
  } else {
    profile = accountStore.mcProfiles.find((profile) => profile.uuid === profileUuid) ?? null;
  }

  if (!profile) {
    alertController.error("Profile not found");
    return null;
  }
  
  return profile;
}