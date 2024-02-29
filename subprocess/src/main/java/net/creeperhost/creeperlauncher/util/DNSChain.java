package net.creeperhost.creeperlauncher.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.covers1624.quack.collection.FastStream;
import okhttp3.*;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 20/2/23.
 */
public final class DNSChain {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("DNSChain.debug");

    private static final String RESOLVE_TEST1 = "resolve.modpacks.ch";
    private static final String RESOLVE_RESULT1 = "127.42.69.66";

    private static final String RESOLVE_TEST2 = "noc.creeper.host";
    private static final String RESOLVE_RESULT2 = "185.57.191.150";

    private static final String CAPTIVE_TEST = "http://captive.modpacks.ch/";
    private static final String CAPTIVE_RESULT = "prepay-liqueur-deodorize-corporate-enzyme-cornea-poking-contend-busybody-spender-reexamine-pellet-sternness-tinwork-syndrome-graves-occupancy-yelling-properly";

    private static final ExecutorService SELF_TEST_EXECUTOR = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("DNS Self Test Executor %d")
            .setDaemon(true)
            .build()
    );

    private final List<StepEntry> steps;
    private final Cache<String, DNSResult> CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public DNSChain(DNSStep... steps) {
        this.steps = FastStream.of(steps)
                .map(StepEntry::new)
                .toImmutableList();

        scheduleSelfTest();
    }

    public InetAddress[] lookup(String host) throws UnknownHostException {
        return query(host).answer.clone();
    }

    public DNSResult query(String host) throws UnknownHostException {
        if (DEBUG) LOGGER.info("DNS query for {}", host);
        DNSResult result = CACHE.getIfPresent(host);
        if (DEBUG && result != null) LOGGER.info(" - Cached {}", (Object) result.answer);
        if (result != null) return result;

        synchronized (host.intern()) {
            result = CACHE.getIfPresent(host);
            if (DEBUG && result != null) LOGGER.info(" - Cached: {}", (Object) result.answer);
            if (result != null) return result;

            UnknownHostException exception = null;
            for (StepEntry entry : steps) {
                if (!entry.isEnabled()) continue;
                DNSStep step = entry.step;
                try {
                    InetAddress[] answer = step.resolve(host);
                    if (DEBUG) LOGGER.info(" - Resolved with {}: {}", step.getName(), answer);

                    result = new DNSResult(host, step.getName(), answer);
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

    public List<StepEntry> getSteps() {
        return steps;
    }

    // TODO, perhaps we should do this on an interval or something??
    public void scheduleSelfTest() {
        for (StepEntry entry : this.steps) {
            entry.scheduleSelfTest();
        }
    }

    public record SelfTestReport(String testType, String message, @Nullable Throwable ex, boolean pass) { }

    public record DNSResult(String host, String resolvedBy, InetAddress[] answer) {
    }

    public static final class StepEntry {

        public final DNSStep step;
        private final List<SelfTestReport> reports = new LinkedList<>();
        private boolean enabled = true;

        @Nullable
        private CompletableFuture<Void> selfTestFuture;

        private StepEntry(DNSStep step) {
            this.step = step;
        }

        public void scheduleSelfTest() {
            if (selfTestFuture != null && !selfTestFuture.isDone()) {
                selfTestFuture.cancel(true);
            }
            selfTestFuture = CompletableFuture.runAsync(this::runSelfTest, SELF_TEST_EXECUTOR);
        }

        private void runSelfTest() {
            SelfTestReport resolve1 = runDnsTest(RESOLVE_TEST1, RESOLVE_RESULT1);
            SelfTestReport resolve2 = runDnsTest(RESOLVE_TEST2, RESOLVE_RESULT2);

            SelfTestReport captive1 = runCaptiveTest(CAPTIVE_TEST, CAPTIVE_RESULT);

            synchronized (reports) {
                reports.clear();
                reports.add(resolve1);
                reports.add(resolve2);
                reports.add(captive1);
            }

            if (!(step instanceof SystemDNSStep)) {
                boolean prev = enabled;
                enabled = resolve1.pass && resolve2.pass && captive1.pass;
                if (prev != enabled) {
                    LOGGER.warn("{} DNS step {}.", enabled ? "Enabling" : "Disabling", step.getName());
                }
            }
        }

        public List<SelfTestReport> getReports() {
            synchronized (reports) {
                return new ArrayList<>(reports);
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Nullable
        public CompletableFuture<Void> getSelfTestFuture() {
            return selfTestFuture;
        }

        private SelfTestReport runDnsTest(String host, String expected) {
            try {
                InetAddress[] answer = step.resolve(host);
                boolean pass = FastStream.of(answer)
                        .anyMatch(e -> e.getHostAddress().equals(expected));
                return new SelfTestReport(
                        "resolve-test: " + host,
                        "Query success." + (!pass ? " Did not receive expected DNS entry: " + expected + " Got: " + Arrays.toString(answer) : ""),
                        null,
                        pass
                );
            } catch (UnknownHostException ex) {
                return new SelfTestReport("resolve-test: " + host, "Query failed.", ex, false);
            }
        }

        private SelfTestReport runCaptiveTest(String url, String expected) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .dns(new OkHttpStepAdapter(step))
                    .build();

            try (Response response = client.newCall(new Request.Builder().url(url).get().build()).execute()) {
                ResponseBody body = response.body();
                String bodyStr = body != null ? body.string() : "";
                if (response.code() != 200) {
                    return new SelfTestReport(
                            "captive-test: " + url,
                            "Query Failed. Expected 200. Got: " + response.code() + ". Body: " + bodyStr,
                            null,
                            false
                    );
                }
                boolean pass = bodyStr.trim().equals(expected);
                return new SelfTestReport(
                        "captive-test: " + url,
                        "Query Success." + (!pass ? " Did not receive expected body: " + expected + " Got: " + bodyStr : ""),
                        null,
                        pass
                );
            } catch (IOException ex) {
                return new SelfTestReport(
                        "captive-test: " + url,
                        "Query Failed.",
                        ex,
                        false
                );
            } finally {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
            }
        }
    }

    public interface DNSStep {

        InetAddress[] resolve(String host) throws UnknownHostException;

        String getName();
    }

    public static class DnsOverHttpsStep implements DNSStep {

        private final DOHHost host;
        private final DnsOverHttps dns;

        public DnsOverHttpsStep(okhttp3.Cache cache, DOHHost host) {
            this.host = host;
            dns = new DnsOverHttps.Builder()
                    .client(new OkHttpClient.Builder().cache(cache).build())
                    .url(HttpUrl.get(host.host))
                    .resolvePrivateAddresses(true)
                    .systemDns(new BoostrapDns(host.alternativeNames, host.bootstrapHosts))
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

    private record OkHttpStepAdapter(DNSStep step) implements okhttp3.Dns {

        @Override
        public List<InetAddress> lookup(String host) throws UnknownHostException {
            return Arrays.asList(step.resolve(host));
        }
    }

    private record BoostrapDns(List<String> hosts, List<InetAddress> answers) implements okhttp3.Dns {

        @Override
        public List<InetAddress> lookup(String s) throws UnknownHostException {
            if (!hosts.contains(s)) {
                throw new UnknownHostException("BoostrapDns called for " + s + " instead of " + hosts);
            }
            return answers;
        }
    }
}
