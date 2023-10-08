package net.creeperhost.creeperlauncher.instance.importer.transformers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.MetaFinal;

@FunctionalInterface
public interface MetaTransformer {
    MetaFinal transform(JsonElement original);
}
