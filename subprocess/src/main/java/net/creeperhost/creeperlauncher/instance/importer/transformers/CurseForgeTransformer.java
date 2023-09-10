package net.creeperhost.creeperlauncher.instance.importer.transformers;

import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.instance.importer.meta.CurseForgeMeta;
import net.creeperhost.creeperlauncher.instance.importer.meta.MetaFinal;


public class CurseForgeTransformer implements MetaTransformer<CurseForgeMeta> {
    @Override
    public MetaFinal transform(CurseForgeMeta original) {
        var lookupId = original.projectId() == null ? -1 : original.projectId();
        var lookupVersionId = original.fileId() == null ? -1 : original.fileId();
        
        // TODO: Lookup project
        
//        InstanceJson instance = InstanceJson.create(
//            lookupId,
//            lookupVersionId,
//            original.name(),
//            original.manifest() != null ? original.manifest().version() : "",
//            original.gameVersion(),
//            4096,
//            6144,
//            original.isMemoryOverride() ? original.allocatedMemory().intValue() : 0 // TODO, set correctly
//            
//        );
        
        return null;
    }
}
