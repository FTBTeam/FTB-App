package net.creeperhost.creeperlauncher.data.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Some older legacy versions of Forge due to branch structure sometimes had odd
 * version names, Specifically, they had the branch name appended.
 * <p>
 * Forge doesn't appear to generate these version names anymore.
 * <p>
 * Created by covers1624 on 14/2/22.
 */
public class VersionOverrides {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Map<String, String> overrides = new LinkedHashMap<>();

    public static VersionOverrides compute() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OkHttpDownloadAction action = new OkHttpDownloadAction()
                    .setClient(Constants.httpClient())
                    .setUrl(Constants.CH_MAVEN + "net/minecraftforge/forge/maven-metadata.xml")
                    .setDest(bos);

            action.execute();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(bos.toByteArray()));
            Element docElement = doc.getDocumentElement();
            docElement.normalize();

            VersionOverrides versionOverrides = new VersionOverrides();

            // Load all versions from maven-metadata
            NodeList nl = docElement.getElementsByTagName("version");
            for (int i = nl.getLength() - 1; i >= 0; i--) {
                Node node = nl.item(i);
                String text = node.getFirstChild().getTextContent();

                // Has more than 2 segments means it has something appended.
                String[] segs = text.split("-");
                if (segs.length > 2) {
                    versionOverrides.overrides.put(segs[1], text);
                }
            }
            return versionOverrides;
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            LOGGER.fatal("Failed to initialize VersionOverrides..", ex);
            return new VersionOverrides();
        }
    }

    @Nullable
    public String find(String forgeVersion) {
        return overrides.get(forgeVersion);
    }
}
