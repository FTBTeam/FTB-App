package dev.ftb.app.structs.accounts;

/// Holds data relating to the active device flow
public record DeviceFlow(String flowId, String deviceCode, long expiresAt, int interval) {
    public DeviceFlow slowedDown() {
        return new DeviceFlow(flowId, deviceCode, expiresAt, interval + 5);
    }
}
