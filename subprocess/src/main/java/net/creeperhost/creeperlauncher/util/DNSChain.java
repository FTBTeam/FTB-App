package net.creeperhost.creeperlauncher.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 20/2/23.
 */
public final class DNSChain {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("DNSChain.debug");

    private final List<DNSStep> steps;
    private final Cache<String, InetAddress[]> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public DNSChain(DNSStep... steps) {
        this.steps = List.of(steps);
    }

    public InetAddress[] lookup(String host) throws UnknownHostException {
        if (DEBUG) LOGGER.info("DNS query for {}", host);
        InetAddress[] result = CACHE.getIfPresent(host);
        if (DEBUG && result != null) LOGGER.info(" - Cached {}", (Object) result);
        if (result != null) return result;

        synchronized (host.intern()) {
            result = CACHE.getIfPresent(host);
            if (DEBUG && result != null) LOGGER.info(" - Cached: {}", (Object) result);
            if (result != null) return result;

            UnknownHostException exception = null;
            for (DNSStep step : steps) {
                try {
                    result = step.resolve(host);
                    if (DEBUG) LOGGER.info(" - Resolved with {}: {}", step.getName(), result);
                    CACHE.put(host, result);
                    return result;
                } catch (UnknownHostException ex) {
                    if (DEBUG) LOGGER.info(" - Tried with {}. ", step.getName(), ex);
                    if (exception != null) {
                        exception.addSuppressed(ex);
                    } else {
                        exception = ex;
                    }
                }
            }

            if (DEBUG) LOGGER.info(" - Resolution failed! Tried all steps. {}", host, exception);
            assert exception != null;
            throw exception;
        }
    }

    public interface DNSStep {

        InetAddress[] resolve(String host) throws UnknownHostException;

        String getName();
    }

    public static class DnsOverHttpsStep implements DNSStep {

        private final DOHHosts host;
        private final DnsOverHttps dns;

        public DnsOverHttpsStep(okhttp3.Cache cache, DOHHosts host) {
            this.host = host;
            dns = new DnsOverHttps.Builder()
                    .client(new OkHttpClient.Builder().cache(cache).build())
                    .url(HttpUrl.get(host.host))
                    .resolvePrivateAddresses(true)
                    .bootstrapDnsHosts(host.bootstrapHosts)
                    .build();
        }

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return dns.lookup(host).toArray(new InetAddress[0]);
        }

        @Override
        public String getName() {
            return host.name();
        }
    }

    public static class SystemDNSStep implements DNSStep {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return InetAddress.getAllByName(host);
        }

        @Override
        public String getName() {
            return "System";
        }
    }

    public record OkHTTPAdapter(DNSChain chain) implements okhttp3.Dns {

        @Override
        public List<InetAddress> lookup(String host) throws UnknownHostException {
            return Arrays.asList(chain.lookup(host));
        }
    }
}
