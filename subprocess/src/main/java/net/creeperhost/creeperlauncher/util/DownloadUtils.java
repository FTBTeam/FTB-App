package net.creeperhost.creeperlauncher.util;

import net.creeperhost.creeperlauncher.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

public class DownloadUtils
{
    public static boolean downloadFile(Path target, String url)
    {
        return downloadFile(target, url, false);
    }

    public static boolean downloadFile(Path target, String url, boolean auth)
    {
        try
        {
            URLConnection connection = getConnection(url, auth);
            if (connection != null && connection.getInputStream() != null)
            {
                Files.copy(connection.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException ignored)
        {
        }
        return false;
    }

    private static URLConnection getConnection(String address)
    {
        return getConnection(address, false);
    }

    private static URLConnection getConnection(String address, boolean auth)
    {
        try
        {
            int MAX = 3;
            URL url = new URL(address);
            URLConnection connection = null;
            for (int x = 0; x < MAX; x++)
            {
                connection = url.openConnection();
                if (auth)
                {
                    if(!Constants.KEY.isEmpty() | !Constants.SECRET.isEmpty())
                    {
                        connection.addRequestProperty("USER_SECRET", Constants.SECRET);
                    }
                }
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                if (connection instanceof HttpURLConnection)
                {
                    HttpURLConnection hcon = (HttpURLConnection) connection;
                    hcon.setInstanceFollowRedirects(false);
                    int res = hcon.getResponseCode();
                    if (res == HttpURLConnection.HTTP_MOVED_PERM || res == HttpURLConnection.HTTP_MOVED_TEMP)
                    {
                        String location = hcon.getHeaderField("Location");
                        hcon.disconnect();
                        url = new URL(url, location);
                    } else
                    {
                        break;
                    }
                } else
                {
                    break;
                }
            }
            return connection;
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String urlToString(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
