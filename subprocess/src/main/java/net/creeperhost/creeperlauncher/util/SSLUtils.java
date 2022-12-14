package net.creeperhost.creeperlauncher.util;

import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SSL Utilities for logging and tracing {@link X509Certificate}s.
 * <p>
 * Created by covers1624 on 22/11/22.
 */
public class SSLUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static X509TrustManager defaultTrustManager;
    private static LoggingX509TrustManager loggingTrustManager;
    private static SSLContext sslCtx;

    static {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null); // Using null initializes using the default KeyStore.
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509TM) {
                    defaultTrustManager = x509TM;
                    loggingTrustManager = new LoggingX509TrustManager(defaultTrustManager);
                    break;
                }
            }
            if (defaultTrustManager == null) {
                LOGGER.warn("Failed to find default X509TrustManager..");
            } else {
                sslCtx = SSLContext.getInstance("TLS");
                sslCtx.init(null, new TrustManager[] { loggingTrustManager }, null);
            }
        } catch (Throwable ex) {
            LOGGER.warn("Failed to retrieve default X509TrustManager.. This should never happen??");
        }
    }

    /**
     * Replaces the default {@link SSLContext} with our variant that passes things
     * through {@link LoggingX509TrustManager}.
     */
    public static void inject() {
        if (sslCtx != null) {
            SSLContext.setDefault(sslCtx);
        }
    }

    /**
     * Overrides the {@link SSLSocketFactory} and {@link X509TrustManager} used by
     * {@link OkHttpClient}, including our certificate chain logging.
     *
     * @param builder The {@link OkHttpClient.Builder}.
     * @return The same builder.
     */
    public static OkHttpClient.Builder inject(OkHttpClient.Builder builder) {
        if (sslCtx != null && loggingTrustManager != null) {
            LOGGER.trace("Injecting LoggingX509TrustManager into OkHTTP.");
            builder.sslSocketFactory(sslCtx.getSocketFactory(), loggingTrustManager);
        }
        return builder;
    }

    /**
     * Gets the current thread's {@link X509Certificate} chain.
     *
     * @return {@code null} if no certificate chain exists.
     */
    @Nullable
    public static X509Certificate[] getThreadCertificates() {
        return LoggingX509TrustManager.certs.get();
    }

    public static void clearThreadCertificates() {
        LoggingX509TrustManager.certs.set(null);
    }

    /**
     * A wrapped {@link X509TrustManager}, which proxies all calls to the provided delegate.
     * <p>
     * Stores the certificate to a {@link ThreadLocal}.
     *
     * @param delegate
     */
    private record LoggingX509TrustManager(X509TrustManager delegate) implements X509TrustManager {

        private static final ThreadLocal<X509Certificate[]> certs = new ThreadLocal<>();

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            certs.set(chain);
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
}
