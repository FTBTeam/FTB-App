package dev.ftb.app.util;

import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

public class ProxyWithAuth extends Proxy {

    @Nullable
    public final PasswordAuthentication auth;

    public ProxyWithAuth(Type type, String host, int port, @Nullable PasswordAuthentication auth) {
        super(type, new InetSocketAddress(host, port));
        this.auth = auth;
    }
}
