package dev.ftb.app.api.data;

public class BaseData {
    public String type;
    public String requestId;
    public String secret;


    // Not super graceful but it works I guess.
    public static void attachReply(BaseData data, BaseData reply) {
        reply.requestId = data.requestId;
        reply.secret = data.secret;
        reply.type = data.type + "Reply";
    }
}
