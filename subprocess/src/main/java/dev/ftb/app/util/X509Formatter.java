package dev.ftb.app.util;

import net.covers1624.quack.io.IndentPrintWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;

/**
 * A very horrible X509 certificate formatter.
 * <p>
 * Vaguely resembles the openssl -text format.
 * <p>
 */
public class X509Formatter {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String printCertificate(X509Certificate cert) {
        StringWriter sw = new StringWriter();
        IndentPrintWriter pw = new IndentPrintWriter(new PrintWriter(sw));
        pw.println("Data:");
        indented(pw, () -> {
            pw.println("Version: " + cert.getVersion());
            pw.println("Serial Number:");
            indented(pw, () -> {
                pw.println(separatedHex(cert.getSerialNumber()));
            });
            pw.println("Issuer: " + cert.getIssuerX500Principal());

            pw.println("Validity");
            indented(pw, () -> {
                pw.println("Not Before: " + cert.getNotBefore());
                pw.println("Not After : " + cert.getNotAfter());
            });

            pw.println("Subject: " + cert.getSubjectX500Principal());

            PublicKey pubKey = cert.getPublicKey();
            pw.println("Subject Public Key Info:");
            indented(pw, () -> {
                pw.println("Public Key Algorithm: " + pubKey.getAlgorithm());
                if (pubKey instanceof RSAPublicKey rsaKey) {
                    indented(pw, () -> {
                        pw.println("RSA Public-Key: (" + rsaKey.getModulus().bitLength() + " bit)");
                        pw.println("Modulus:");
                        indented(pw, () -> printBlockedHex(pw, rsaKey.getModulus()));
                        pw.println("Exponent: " + rsaKey.getPublicExponent());
                    });
                }
            });
            try {
                printAlternativeNames(pw, cert.getSubjectAlternativeNames());
            } catch (CertificateParsingException ignored) {
            }
        });
        pw.println("Signature Algorithm: " + cert.getSigAlgName());
        indented(pw, () -> printBlockedHex(pw, cert.getSignature()));

        pw.flush();
        return sw.toString();
    }

    private static void printAlternativeNames(IndentPrintWriter pw, Collection<List<?>> alternativeNames) {
        pw.println("Subject Alternative Names:");
        indented(pw, () -> {
            for (List<?> altName : alternativeNames) {
                Integer i = (Integer) altName.get(0);
                switch (i) {
                    case 0 -> pw.print("Other:");
                    case 1 -> pw.print("RFC822 Name:");
                    case 2 -> pw.print("DNS:");
                    case 3 -> pw.print("OR Address:");
                    case 4 -> pw.print("Name:");
                    case 5 -> pw.print("EDI Party Name:");
                    case 6 -> pw.print("URI:");
                    case 7 -> pw.print("IP:");
                    case 8 -> pw.print("ID:");
                    default -> pw.print("UNKNOWN:");
                }
                pw.println(altName.get(1));
            }
        });
    }

    private static void indented(IndentPrintWriter pw, Runnable action) {
        pw.pushIndent();
        action.run();
        pw.popIndent();
    }

    private static String separatedHex(BigInteger i) {
        return separatedHex(i.toString(16));
    }

    private static String separatedHex(String hexString) {
        StringBuilder sb = new StringBuilder();
        char[] chars = hexString.toCharArray();
        for (int idx = 0; idx < chars.length; idx += 2) {
            if (idx != 0) {
                sb.append(":");
            }
            sb.append(chars[idx]);
            if (idx + 1 < chars.length) {
                sb.append(chars[idx + 1]);
            }
        }
        return sb.toString();
    }

    private static void printBlockedHex(PrintWriter pw, byte[] bytes) {
        printBlockedHex(pw, toHex(bytes));
    }

    private static void printBlockedHex(PrintWriter pw, BigInteger i) {
        printBlockedHex(pw, i.toString(16));
    }

    private static void printBlockedHex(PrintWriter pw, String hex) {
        StringBuilder sb = new StringBuilder();
        char[] chars = hex.toCharArray();
        int groups = 0;
        for (int idx = 0; idx < chars.length; idx += 2) {
            if (groups == 16) {
                pw.println(sb);
                sb.setLength(0);
                groups = 0;
            } else if (idx != 0) {
                sb.append(":");
            }
            sb.append(chars[idx]);
            if (idx + 1 < chars.length) {
                sb.append(chars[idx + 1]);
                groups++;
            }
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[2 * bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[2 * i] = HEX_CHARS[(bytes[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[bytes[i] & 0x0F];
        }
        return new String(chars);
    }
}
