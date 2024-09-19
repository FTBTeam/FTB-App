import {AuthProfile, CoreState} from '@/modules/core/core.types';
import store from '@/modules/store';
import {alertController} from '@/core/controllers/alertController';

/**
 * Selects the active profile or attempts to resolve the profile by UUID
 * If no profile is found, an error is shown and null is returned
 * 
 * @param profileUuid The UUID of the profile to select
 */
export function getProfileOrDefaultToActive(profileUuid: string | null = null): AuthProfile | null {
  let profile: AuthProfile | null = null;
  if (profileUuid === null) {
    const activeProfile = (store.state["core"] as CoreState).activeProfile;
    if (!activeProfile) {
      alertController.error("No active profile selected");
      return null;
    }

    profile = activeProfile;
  } else {
    profile = (store.state["core"] as CoreState).profiles.find((profile) => profile.uuid === profileUuid) ?? null;
  }

  if (!profile) {
    alertController.error("Profile not found");
    return null;
  }
  
  return profile;
}