package dev.ftb.app.api.data.other;

import dev.ftb.app.api.data.BaseData;

public class FileHashData extends BaseData {
    public String uuid;
    public String filePath;

    public static class Reply extends BaseData {
        public String md5Hash;
        public String shaHash;
        public Reply(FileHashData data, String md5Hash, String shaHash)
        {
            requestId = data.requestId;
            this.md5Hash = md5Hash;
            this.shaHash = shaHash;
        }
    }
}
