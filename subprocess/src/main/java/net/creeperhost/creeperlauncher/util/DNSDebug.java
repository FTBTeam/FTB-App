package net.creeperhost.creeperlauncher.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IndentPrintWriter;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by covers1624 on 22/11/22.
 */
public final class DNSDebug {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("DNS Report Executor %d")
            .setDaemon(true)
            .build()
    );

    private static final String[] IMPORTANT_HOSTS = new String[] {
            "sessionserver.mojang.com",
            "launchermeta.mojang.com",
            "api.mojang.com",
            "session.minecraft.net",
            "piston-meta.mojang.com",
            "api.modpacks.ch",
            "apps.modpacks.ch",
            "dist.modpacks.ch",
            "api.creeperhost.net",
            "api.creeper.host",
            "maven.creeperhost.net",
            "maven.fabricmc.net",
            "maven.minecraftforge.net",
            "maven.neoforged.net"
    };

    @Nullable
    private static CompletableFuture<Void> executingReport;

    public static void printDebugReport() {
        if (executingReport != null && !executingReport.isDone()) {
            executingReport.cancel(true);
        }
        executingReport = CompletableFuture.runAsync(DNSDebug::doPrintDebugReport, EXECUTOR);
    }

    private static void doPrintDebugReport() {
        StringWriter sw = new StringWriter();
        IndentPrintWriter pw = new IndentPrintWriter(new PrintWriter(sw));
        pw.println("DNS Report:");
        pw.println("Chain:");
        for (DNSChain.StepEntry step : Constants.DNS_CHAIN.getSteps()) {
            CompletableFuture<Void> future = step.getSelfTestFuture();
            if (future == null) continue; // ??? should never happen..
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
            if (!future.isDone()) continue; // ??? Should also never happen..

            pw.println(step.step.getName() + " - " + (step.isEnabled() ? "ENABLED" : "!!DISABLED!!") + " - Reports:");
            for (DNSChain.SelfTestReport report : step.getReports()) {
                pw.pushIndent();
                pw.println("Test: " + report.testType() + " - " + (report.pass() ? "Success" : "Failure"));
                pw.pushIndent();
                pw.println("Result: " + report.message());
                Throwable ex = report.ex();
                if (ex != null) {
                    pw.println("Exception:");
                    pw.pushIndent();
                    ex.printStackTrace(pw);
                    pw.popIndent();
                }
                pw.popIndent();
                pw.popIndent();
            }
        }
        pw.println("Important Hosts:");
        for (String host : IMPORTANT_HOSTS) {
            pw.println(host);
            pw.pushIndent();
            try {
                DNSChain.DNSResult result = Constants.DNS_CHAIN.query(host);
                pw.println("From  : " + result.resolvedBy());
                pw.println("Answer: [" + FastStream.of(result.answer()).map(InetAddress::getHostAddress).join(", ") + "]");
            } catch (Throwable ex) {
                pw.println("Request failed.");
                pw.pushIndent();
                ex.printStackTrace(pw);
                pw.popIndent();
            }
            pw.popIndent();
        }
        pw.flush();
        LOGGER.info(sw.toString());
    }
}
