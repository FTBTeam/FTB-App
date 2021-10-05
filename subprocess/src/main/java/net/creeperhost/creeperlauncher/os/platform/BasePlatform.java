package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.os.Platform;
import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 8/3/21.
 */
abstract class BasePlatform implements Platform {

    private final IWindowHelper windowHelper;

    protected BasePlatform(IWindowHelper windowHelper) {
        this.windowHelper = windowHelper;
    }

    @Override
    public final IWindowHelper getWindowHelper() {
        return windowHelper;
    }

    protected List<String> prepareLauncherProcessArgs() {
        List<String> args = new ArrayList<>();
        args.add(getLauncherExecutable().toAbsolutePath().toString());
        args.add("--workDir");
        args.add(Constants.BIN_LOCATION.toAbsolutePath().toString());
        return args;
    }

    protected void prepareLauncherEnvironment(ProcessBuilder builder) {
        Map<String, String> environment = builder.environment();
        environment.remove("_JAVA_OPTIONS");
        environment.remove("JAVA_TOOL_OPTIONS");
        environment.remove("JAVA_OPTIONS");
    }

    @Override
    public ProcessBuilder buildLauncherProcess() {
        ProcessBuilder builder = new ProcessBuilder(prepareLauncherProcessArgs());
        prepareLauncherEnvironment(builder);
        return builder;
    }
}
