import { Action, ActionContext, ActionType } from '../protocolActions';
import router, { RouterNames } from '@/router';

export class ModpackInstallAction implements Action {
  namespace: ActionType = 'modpack';
  action = 'install';

  async run(context: ActionContext) {
    const packId = context.query.get('packId') || context.args[0];
    const versionId = (context.query.get('versionId') || context.args[1]) ?? null;
    const type = (context.query.get('type') || context.args[2]) ?? '0'; // default to ftb packs

    if (packId == null) {
      return;
    }

    const payload: any = {
      modpackid: packId,
      type,
    };

    if (versionId) {
      payload.version = versionId;
      payload.showInstall = true;
    }

    await router.push({
      name: RouterNames.ROOT_PREVIEW_PACK,
      query: payload,
    });
  }
}
