package dev.ftb.app.api.data.other;

import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.api.data.BaseData;
import net.covers1624.quack.gson.PathTypeAdapter;

import java.nio.file.Path;

public class UploadLogsData extends BaseData {
    public UploadLogsData() {
        type = "uploadLogs";
    }

    public static class Reply extends BaseData {
        private final boolean error;
        @JsonAdapter(PathTypeAdapter.class)
        private Path path;

        public Reply(String requestId, Path path) {
            this.requestId = requestId;
            this.path = path;
            this.type = "uploadLogsReply";
            this.error = false;
        }

        public Reply(String requestId) {
            this.requestId = requestId;
            this.error = true;
        }
    }
}
