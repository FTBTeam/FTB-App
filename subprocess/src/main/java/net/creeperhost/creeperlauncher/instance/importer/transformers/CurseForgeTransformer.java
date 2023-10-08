package net.creeperhost.creeperlauncher.instance.importer.transformers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.MetaFinal;

public class CurseForgeTransformer implements MetaTransformer {
    @Override
    public MetaFinal transform(JsonElement original) {
//        var lookupId = original.projectId() == null ? -1 : original.projectId();
//        var lookupVersionId = original.fileId() == null ? -1 : original.fileId();
        
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
