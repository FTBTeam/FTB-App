package dev.ftb.app.api.handlers.other.minetogether;

public class S3Credentials {
    public String accessKeyId;
    public String secretAccessKey;
    public String bucket;
    public String host;

    public S3Credentials(String accessKeyId, String secretAccessKey, String bucket, String host) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.host = host;
    }
}
