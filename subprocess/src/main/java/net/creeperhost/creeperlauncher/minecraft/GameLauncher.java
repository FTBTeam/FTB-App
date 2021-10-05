package net.creeperhost.creeperlauncher.minecraft;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.os.Platform;
import net.creeperhost.creeperlauncher.os.platform.window.IMonitor;
import net.creeperhost.creeperlauncher.os.platform.window.IWindow;
import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class GameLauncher
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static void downloadLauncherProfiles() {
        try {
            Path file = Constants.BIN_LOCATION.resolve(Constants.LAUNCHER_PROFILES_JSON_NAME);
            if(Files.notExists(file)) {
                //Some reason the vanilla launcher is not creating the launcher_profiles.json
                DownloadableFile defaultConfig = new DownloadableFile("", file, "https://apps.modpacks.ch/FTB2/launcher_profiles.json", Collections.emptyList(), 0, 0, "config", "launcher_profiles.json", "");
                defaultConfig.prepare();
                defaultConfig.download(file, true, false, null);
            }
        } catch (Throwable ignored) {
        }

    }

    //TODO, move this to half-somewhat platform specific place.
    public static void tryAutomation(Process start) {
        Platform platform = OS.CURRENT.getPlatform();
        IWindowHelper windowHelper = platform.getWindowHelper();
        long pid = start.pid();
        if (windowHelper.isSupported()) {
            outer:
            while(start.isAlive()) {
                List<IWindow> windows = windowHelper.getWindows((int) pid);
                for (IWindow window : windows) {
                    if (window.getWindowTitle().equals("Minecraft Launcher")) {
                        Rectangle rect = window.getRect();
                        try {
                            Robot robot;
                            IMonitor monitor = window.getMonitor();
                            GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                            GraphicsDevice device = null;
                            if (monitor != null)
                            {
                                device = screenDevices[monitor.getNumber()];
                            } else {
                                free:
                                for (GraphicsDevice graphicsDevice : screenDevices) {
                                    GraphicsConfiguration[] configurations = graphicsDevice.getConfigurations();
                                    for (GraphicsConfiguration configuration : configurations) {
                                        Rectangle bounds1 = configuration.getBounds();
                                        if (bounds1.contains(rect)) {
                                            device = graphicsDevice;
                                            break free;
                                        }
                                    }
                                }
                            }

                            if(device == null)
                                break outer;

                            robot = new Robot(device);
                            Rectangle bounds;
                            double dpiChange = 1;
                            if (monitor != null)
                            {
                                bounds = monitor.getBounds();
                                dpiChange = monitor.getBounds().getWidth() / device.getDefaultConfiguration().getBounds().getWidth();
                            } else {
                                bounds = device.getDefaultConfiguration().getBounds();
                            }

                            Rectangle DPIBounds = new Rectangle((int)(bounds.x / dpiChange), (int)(bounds.y / dpiChange), (int)(bounds.width / dpiChange), (int)(bounds.height / dpiChange));

                            Rectangle relativeRect = new Rectangle(rect.x - bounds.x, rect.y - bounds.y, rect.width, rect.height);

                            Rectangle adjustedRect = new Rectangle(DPIBounds.x + (int)(relativeRect.x / dpiChange), DPIBounds.y + (int)(relativeRect.y / dpiChange), (int)(relativeRect.width / dpiChange), (int)(relativeRect.height / dpiChange));

                            int rectxDPI = adjustedRect.x;
                            int rectyDPI = adjustedRect.y;
                            int widthDPI = adjustedRect.width;
                            int heightDPI = adjustedRect.height;

                            int sideBarleft = 181;

                            int bottomOffset = 30;
                            int topOffset = heightDPI - bottomOffset;
                            int leftOffset = sideBarleft + ((widthDPI - sideBarleft) / 2);

                            int x = rectxDPI + leftOffset;
                            int y = rectyDPI + topOffset;

                            boolean isGreen = false;
                            int count = 0;

                            for(count = 0; count < 250; count++)
                            {
                                window.bringToFront();
                                Color pixelColor = window.getPixelColour(x, y);
                                int red = pixelColor.getRed();
                                int green = pixelColor.getGreen();
                                int blue = pixelColor.getBlue();
                                isGreen = red == 0 && blue > 50 && blue < 80 && green > 130;
                                if (isGreen) break;
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                }
                            }

                            if (!isGreen) break outer; // abort

                            try {
                                Thread.sleep(80);
                            } catch (InterruptedException ignored) {}

                            robot.keyPress(KeyEvent.VK_SHIFT);
                            robot.keyPress(KeyEvent.VK_TAB);
                            robot.keyRelease(KeyEvent.VK_TAB);
                            robot.keyRelease(KeyEvent.VK_SHIFT);

                            robot.keyPress(KeyEvent.VK_SHIFT);
                            robot.keyPress(KeyEvent.VK_TAB);
                            robot.keyRelease(KeyEvent.VK_TAB);
                            robot.keyRelease(KeyEvent.VK_SHIFT);

                            robot.keyPress(KeyEvent.VK_SPACE);
                            robot.keyRelease(KeyEvent.VK_SPACE);
                            robot.keyPress(KeyEvent.VK_SPACE);
                            robot.keyRelease(KeyEvent.VK_SPACE);

                            //window.setPos(buttonCentre, yMove);
                            break outer;
                        } catch (AWTException e) {
                            // shrug
                        }
                    } else {
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }
}
