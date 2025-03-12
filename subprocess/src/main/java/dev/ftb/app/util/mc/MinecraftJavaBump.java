package dev.ftb.app.util.mc;

public enum MinecraftJavaBump {
    JAVA_8(8, "21w19a"),
    JAVA_16(16, "1.18-pre1"),
    JAVA_17(17, "24w14potato"),
    JAVA_21(21, "999999");
    
    public static final String MAGIC_SKIP_VERSION = "999999";
    
    private final int javaVersion;
    private final String untilVersion;
    
    MinecraftJavaBump(int javaVersion, String untilVersion) {
        this.javaVersion = javaVersion;
        this.untilVersion = untilVersion;
    }
    
    public int getJavaVersion() {
        return javaVersion;
    }
    
    public String getUntilVersion() {
        return untilVersion;
    }
}
