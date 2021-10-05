package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class SettingsInfoData extends BaseData
{
    private static SystemInfo si = new SystemInfo();
    private static HardwareAbstractionLayer hal = si.getHardware();

    public static class Reply extends SettingsInfoData
    {
        public HashMap<String, String> settingsInfo;
        public long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        public long availableMemory = hal.getMemory().getAvailable() / 1024 / 1024;
        public int totalCores = hal.getProcessor().getLogicalProcessorCount();
        public Dimension autoResolution = new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() / 2, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight() / 2);
        public HashSet<Dimension> supportedResolutions = new HashSet<>();

        public Reply(SettingsInfoData data, HashMap<String, String> settingsInfo)
        {
            type = "settingsInfoReply";
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getScreenDevices();
            for (int i = 0; i < devices.length; i++)
            {
                GraphicsDevice dev = devices[i];
                DisplayMode[] modes = dev.getDisplayModes();
                for (int j = 0; j < modes.length; j++)
                {
                    DisplayMode m = modes[j];
                    supportedResolutions.add(new Dimension(m.getWidth(), m.getHeight()));
                }
            }
            requestId = data.requestId;
            this.settingsInfo = settingsInfo;

        }
    }
}
