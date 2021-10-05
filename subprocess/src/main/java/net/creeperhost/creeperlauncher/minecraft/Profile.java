package net.creeperhost.creeperlauncher.minecraft;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;

import static net.creeperhost.creeperlauncher.util.ImageUtils.resizeImage;

public class Profile
{
    private static final Logger LOGGER = LogManager.getLogger();

    private String name;
    private String mcVersion;
    private String lastVersionId;
    private String lastUsed;
    private String type;
    private Path gameDir;
    private String ID;
    private String javaArgs;
    private String icon;
    private McResolution resolution;

    public Profile(String ID, String name, String mcVersion, String lastVersionId, String lastUsed, String type, Path gameDir, String icon, String args, int ram, int width, int height)
    {
        this.name = name;
        this.mcVersion = mcVersion;
        this.lastVersionId = lastVersionId;
        if((lastVersionId == null || lastVersionId.isEmpty()) && (mcVersion == null || mcVersion.isEmpty())) this.lastVersionId = mcVersion;
        this.lastUsed = lastUsed;
        this.type = type;
        this.gameDir = gameDir;
        this.ID = ID;
        if(ram == 0) ram = 1024;
        this.javaArgs = ("-Xmx" + ram + "M -Duser.language=en-GB " + args.trim()).trim();

        if(icon != null && !icon.isEmpty()) {
            String[] img = icon.split(",");
            byte[] imageByte = img[1].getBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(imageByte));
            try {
                BufferedImage art = resizeImage(ImageIO.read(bis), 32, 32);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(art, "png", bos);
                this.icon = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
            } catch (Throwable e) {
                LOGGER.warn("Unable to resize pack art for Mojang launcher.", e);
                this.icon = icon;
            }
        }
        this.resolution = new McResolution(width, height);
    }

    public JsonObject toJsonObject()
    {
        return GsonUtils.GSON.fromJson(GsonUtils.GSON.toJson(this), JsonObject.class);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getID()
    {
        return ID;
    }

    class McResolution
    {
        int width;
        int height;

        public McResolution(int width, int height)
        {
            this.width = width;
            this.height = height;
        }
    }
}

