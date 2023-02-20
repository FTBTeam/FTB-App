package net.creeperhost.creeperlauncher.util;

import com.google.common.collect.ImmutableList;
import net.covers1624.quack.collection.StreamableIterable;
import okhttp3.HttpUrl;

import java.net.InetAddress;
import java.util.List;

import static net.covers1624.quack.util.SneakyUtils.sneak;

/**
 * Created by covers1624 on 20/2/23.
 */
public enum DOHHost {
    CLOUDFLARE("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1"),
    GOOGLE("https://dns.google.com/dns-query", List.of("dns.google"), "8.8.8.8", "8.8.4.4"),
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
     * Google is currently the only snowflake which triggers OkHTTP to
     * redirect to 'dns.google', thus we need it as an alternative..
     */
    public final List<String> alternativeNames;
    /**
     * The static ips for the hostname.
     */
    public final List<InetAddress> bootstrapHosts;

    DOHHost(String host, String... bootstrapHosts) {
        this(host, List.of(), bootstrapHosts);
    }

    DOHHost(String host, List<String> alternativeNames, String... bootstrapHosts) {
        this.host = host;
        this.alternativeNames = new ImmutableList.Builder<String>().add(HttpUrl.get(host).host()).addAll(alternativeNames).build();
        this.bootstrapHosts = StreamableIterable.of(bootstrapHosts).map(sneak(InetAddress::getByName)).toImmutableList();
    }
}
