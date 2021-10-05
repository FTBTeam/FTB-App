package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.pack.ModPack;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class InstanceInfoData extends BaseData
{
    public String uuid;
    public Dimension autoResolution = new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth() / 2, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight() / 2);
    public HashSet<Dimension> supportedResolutions = new HashSet<>();


    public static class Reply extends InstanceInfoData
    {
        public HashMap<String, String> instanceInfo;
        ModPack packInfo;

        public Reply(InstanceInfoData data, HashMap<String, String> instanceInfo, ModPack packInfo)
        {
            type = "instanceInfoReply";
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
            uuid = data.uuid;
            requestId = data.requestId;
            this.instanceInfo = instanceInfo;
            this.packInfo = packInfo;
            // TODO: configurable options in InstanceInfoDataReply
        }
    }
}
