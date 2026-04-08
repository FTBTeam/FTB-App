package dev.ftb.app.util;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.LinkedList;
import java.util.List;

public class SimpleCookieJar implements CookieJar {

    private final List<Cookie> cookies = new LinkedList<>();

    @Override
    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        cookies.addAll(list);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> ret = new LinkedList<>();
        for (Cookie cookie : cookies) {
            if (cookie.matches(httpUrl)) {
                ret.add(cookie);
            }
        }
        return ret;
    }
}
