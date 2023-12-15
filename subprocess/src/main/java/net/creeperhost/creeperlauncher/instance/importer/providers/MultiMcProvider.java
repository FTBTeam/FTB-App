//package net.creeperhost.creeperlauncher.instance.importer.providers;
//
//import net.covers1624.quack.platform.OperatingSystem;
//import org.jetbrains.annotations.Nullable;
//
//import java.nio.file.Path;
//
//public class MultiMcProvider extends PrismLikeProvider {
//    @Override
//    public @Nullable Path getDataLocation() {
//        return switch (OperatingSystem.current()) {
//            case WINDOWS -> throw new RuntimeException("Not implemented");
//            case LINUX, SOLARIS, FREEBSD -> throw new RuntimeException("Not implemented");
//            case MACOS -> Path.of("/Applications/MultiMC.app/Data/instances/");
//            default -> null;
//        };
//    }
//}
