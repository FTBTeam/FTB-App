package net.creeperhost.creeperlauncher.data.modpack;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Partially models the <code>modpacks.ch</code> api.
 * <p>
 * The data modeled here is only the data required for app operation
 * and does not reflect the entire spec.
 * <p>
 * Created by covers1624 on 16/11/21.
 */
public class ModpackManifest {

    @Nullable
    public String status;

    public int id;
    @Nullable
    public String name;
    @Nullable
    public String description;
    public List<Author> authors = new LinkedList<>();
    public List<Art> art = new LinkedList<>();

    public static class Author {

        @Nullable
        public String name;
    }

    public static class Art {

        @Nullable
        public String type;
        @Nullable
        public String url;
    }
}
