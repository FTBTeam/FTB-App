package net.creeperhost.creeperlauncher.share;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.pack.ModPack;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstanceData
{
    private transient static final Logger LOGGER = LogManager.getLogger();

    private String packName;
    private String minecraftVersion;
    private String loaderVersion;
    private List<FileData> files = new ArrayList<>();
    private transient LocalInstance instance;
    private long memory;
    private long minMemory;
    private transient String code;

    public InstanceData(LocalInstance instance) throws IOException
    {
        this.instance = instance;
        this.packName = instance.getName();
        this.minecraftVersion = instance.getMcVersion();
        this.loaderVersion = instance.getModLoader();
        this.memory = instance.memory;
        this.minMemory = instance.getMinMemory();
        List<Path> files =  Files.walk(instance.getDir(), Integer.MAX_VALUE).collect(Collectors.toList());
        for(Path file : files)
        {
            FileData tmp = new FileData();
            tmp.name = file.getFileName().toString();
            tmp.path = file.toString();
            tmp.size = Files.size(file);
            tmp.checksum = FileUtils.getHash(file, "SHA-1");
            this.files.add(tmp);
        }
    }

    public String share()
    {
        this.files = upload(getFileDataList());
        return this.code;
    }

    public static void main(String[] args)
    {
        CreeperLauncher.initSettingsAndCache();
        Instances.refreshInstances();
        ModPack pack = FTBModPackInstallerTask.getPackFromAPI(285109, 2935316, false, (byte) 1);
        LocalInstance localInstance = new LocalInstance(pack, 2935316, false, (byte) 1);
        FTBModPackInstallerTask install = localInstance.install();
        install.execute().join();
//        install.currentTask.join();
        System.out.println(localInstance.getDir());


//        LocalInstance localInstance = Instances.getInstance(UUID.fromString("8841ce90-66d4-4939-a576-25f75153c702"));
//        InstanceData instanceData = null;
//        try
//        {
//            instanceData = new InstanceData(localInstance);
//        } catch (IOException e) { e.printStackTrace(); }
//        List<FileData> outPut = instanceData.upload(instanceData.getFileDataList());
//
//        if(!outPut.isEmpty())
//        {
//            for (FileData fileData : outPut)
//            {
//                System.out.println("Name: " + fileData.getName() + " URL: " + fileData.getURL());
//            }
//        }
//        instanceData.files = outPut;
//
//        String URL = "https://api.modpacks.ch/" + Constants.KEY + "/modpack/share/" + instanceData.code;
//        String json = GsonUtils.GSON.toJson(instanceData);
//        String resp = WebUtils.putWebResponse(URL, json, true, false);
//
//        System.out.println(URL);
//        System.out.println(resp);

    }

    private List<FileData> upload(List<FileData> files)
    {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost("https://transfer.ch.tools");

        String transferKey = null;

        MultipartEntity mpEntity = new MultipartEntity();

        for (FileData fileData : files)
        {
            if(!Files.isDirectory(Path.of(fileData.getFilePath())))
            {
                System.out.println("Uploading " + Path.of(fileData.getFilePath()).getFileName());
                mpEntity.addPart("file", new FileBody(Path.of(fileData.path).toFile()));
            }
        }

        httppost.setEntity(mpEntity);
        System.out.println("executing request " + httppost.getRequestLine());
        HttpResponse response = null;
        try
        {
            response = httpClient.execute(httppost);
        } catch (IOException e) { e.printStackTrace(); }
        HttpEntity resEntity = response.getEntity();

        System.out.println(response.getStatusLine() + " Files Uploaded");
        System.out.println(response);

        String responseStr = null;

        if (resEntity != null)
        {
            try
            {
                System.out.println("Decoding response entity.");
                responseStr = EntityUtils.toString(resEntity);
            } catch (IOException e) { e.printStackTrace(); }
            System.out.println("Splitting string to get unique code.");
            transferKey = responseStr.split("/")[3];
        }
        if (resEntity != null)
        {
            try
            {
                resEntity.consumeContent();
            } catch (IOException e) { e.printStackTrace(); }
        }

        httpClient.getConnectionManager().shutdown();

        List<FileData> copy = new ArrayList<>(files);
        List<FileData> returnList = new ArrayList<>();
        this.code = transferKey;
        for(FileData fileData : copy)
        {
            if(transferKey != null)
            {
                try
                {
                    if(!Files.isDirectory(Path.of(fileData.getFilePath())))
                    {
                        URL url = new URL("https://transfer.ch.tools/" + transferKey + "/" + fileData.getName());
                        URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                        fileData.url = uri.toASCIIString();
                        String newPath = fileData.getFilePath().toString().replace(instance.getDir().toString(), "").replace("\\", "/").replace(fileData.getName(), "");
                        fileData.path = "." + newPath;
                        returnList.add(fileData);
                    }
                } catch (MalformedURLException | URISyntaxException e) { e.printStackTrace(); }
            }
        }
        return returnList;
    }

    public String getPackName()
    {
        return packName;
    }

    public String getMinecraftVersion()
    {
        return minecraftVersion;
    }

    public String getLoaderVersion()
    {
        return loaderVersion;
    }

    public List<FileData> getFileDataList()
    {
        return files;
    }

    public static class FileData
    {
        String name;
        String path;
        long size;
        String url;
        String checksum;

        public String getName()
        {
            return name;
        }

        public String getFilePath()
        {
            return path;
        }

        public long getSize()
        {
            return size;
        }

        public String getURL()
        {
            return url;
        }

        public String getCheckSum()
        {
            return checksum;
        }
    }
}
