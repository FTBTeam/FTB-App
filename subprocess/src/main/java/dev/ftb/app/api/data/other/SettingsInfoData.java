package dev.ftb.app.api.data.other;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.storage.settings.SettingsData;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public class SettingsInfoData extends BaseData
{
    private static SystemInfo si = new SystemInfo();
    private static HardwareAbstractionLayer hal = si.getHardware();

    @SuppressWarnings("unused")
    public static class Reply extends SettingsInfoData {
        public SettingsData settingsInfo;
        public long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        public long availableMemory = hal.getMemory().getAvailable() / 1024 / 1024;
        public int totalCores = hal.getProcessor().getLogicalProcessorCount();

        public Reply(SettingsInfoData data, SettingsData settingsInfo)
        {
            type = "settingsInfoReply";

            requestId = data.requestId;
            this.settingsInfo = settingsInfo;

        }
    }
}
