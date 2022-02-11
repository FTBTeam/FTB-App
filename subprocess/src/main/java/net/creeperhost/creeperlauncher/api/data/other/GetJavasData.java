package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class GetJavasData extends BaseData {

    public static class Reply extends BaseData {

        public final List<JavaInstall> javas;

        public Reply(GetJavasData data, List<Pair<String, String>> javas) {
            this.requestId = data.requestId;
            this.javas = javas.stream()
                    .map(e -> new JavaInstall(e.getKey(), e.getValue()))
                    .toList();
        }
    }

    public static class JavaInstall {

        public final String name;
        public final String path;

        public JavaInstall(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
