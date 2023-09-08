package net.creeperhost.creeperlauncher.instance.importer.transformers;

import net.creeperhost.creeperlauncher.instance.importer.meta.MetaFinal;

@FunctionalInterface
public interface MetaTransformer<T> {
    MetaFinal transform(T original);
}
