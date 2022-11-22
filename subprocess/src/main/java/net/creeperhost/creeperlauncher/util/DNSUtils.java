package net.creeperhost.creeperlauncher.util;

import net.covers1624.quack.collection.StreamableIterable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;

/**
 * Created by covers1624 on 22/11/22.
 */
public class DNSUtils {

    public static final Logger LOGGER = LogManager.getLogger();

    public static final String[] IMPORTANT_HOSTS = new String[] {
            "authserver.mojang.com",
            "sessionserver.mojang.com",
            "launchermeta.mojang.com",
            "api.mojang.com",
            "session.minecraft.net"
    };

    public static void logImportantHosts() {
        for (String host : IMPORTANT_HOSTS) {
            try {
                String resolved = StreamableIterable.of(InetAddress.getAllByName(host)).map(InetAddress::getHostAddress).join(", ");
                LOGGER.info("{} resolves to [{}]", host, resolved);
            } catch (Throwable ex) {
                LOGGER.error("Failed to resolve host {}.", host, ex);
            }
        }
    }
}
