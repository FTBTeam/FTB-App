package net.creeperhost.creeperlauncher.util;

import net.covers1624.quack.collection.StreamableIterable;

import java.net.InetAddress;
import java.util.List;

import static net.covers1624.quack.util.SneakyUtils.sneak;

/**
 * Created by covers1624 on 20/2/23.
 */
public enum DOHHosts {
    CLOUDFLARE("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1"),
    GOOGLE("https://dns.google.com/dns-query", "8.8.8.8", "8.8.4.4"),
    // Just a transparent proxy in front of cloudflare with an IP ssl cert.
    // This is used as the last fallback to get around ISPs being braindead and
    // wanting to un-secure the internet, and completely blocking 1.1.1.1 and 8.8.8.8.
    CREEPERHOST("https://84.54.54.131/dns-query", "84.54.54.131");

    /**
     * The url to query.
     * <p>
     * No DNS is used to resolve the host in this url, the provided
     * static {@code boostrapHosts} are used instead.
     */
    public final String host;
    /**
     * The static ips for the hostname.
     */
    public final List<InetAddress> bootstrapHosts;

    DOHHosts(String host, String... bootstrapHosts) {
        this.host = host;
        this.bootstrapHosts = StreamableIterable.of(bootstrapHosts).map(sneak(InetAddress::getByName)).toImmutableList();
    }
}
