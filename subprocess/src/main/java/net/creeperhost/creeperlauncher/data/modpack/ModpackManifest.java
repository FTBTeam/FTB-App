package net.creeperhost.creeperlauncher.data.modpack;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Models the <code>modpacks.ch</code> api.
 * Reference: https://modpacksch.docs.apiary.io/#/reference/0/modpacks/modpack-manifest/200?mc=reference%2F0%2Fmodpacks%2Fmodpack-manifest%2F200
 * <p>
 * Created by covers1624 on 16/11/21.
 */
public class ModpackManifest {

    @Nullable
    public String status;

    public long id;
    @Nullable
    public String name;
    @Nullable
    public String description;
    @Nullable
    public String synopsis;
    public List<Author> authors = new LinkedList<>();
    public List<Art> art = new LinkedList<>();

    public static class Author {

        public long id;

        @Nullable
        public String website;
        @Nullable
        public String name;

        @Nullable
        public String type;

        public long updated;
    }

    public static class Art {

        public long id;

        public int width;
        public int height;
        @Nullable
        public String url;
        @Nullable
        public String sha1;
        public int size;

        @Nullable
        public String type;

        public long updated;
    }

    public static class Version {

        public long id;
        @Nullable
        public Specs specs;
        @Nullable
        public String name;
        @Nullable
        public String type;

        public long updated;
    }

    public static class Specs {

        public long id;

        public int minimum;
        public int recommended;
    }
}
