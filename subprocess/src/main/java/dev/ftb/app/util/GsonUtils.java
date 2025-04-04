package dev.ftb.app.util;

import com.google.common.hash.HashCode;
import com.google.gson.*;
import net.covers1624.quack.gson.HashCodeAdapter;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

// TODO move away from global GSON instance, instead have specialized instances.
public class GsonUtils {

    public static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeAdapter(VersionManifest.OS.class, new VersionManifest.OsDeserializer())
            .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
            .create();

    /**
     * Deserializes a Json file from the given Path as the given Type.
     * <p>
     * Type can either be a static field using a Gson TypeToken for classes with Generic parameters as shown here:<br>
     * <code>Type type = new com.google.gson.reflect.TypeToken&lt;Map&lt;String,String&gt;&gt;(){}.getType();</code><br>
     * Or a Class instance such as: <code>JsonObject.class</code>
     *
     * @param path The Path to read the json from.
     * @param type The Type to Deserialize to.
     * @return The Deserialized object.
     * @throws IOException        Thrown if there was an IO error.
     * @throws JsonParseException Thrown if the json fails to parse.
     */
    public static <T> T loadJson(Path path, Type type) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, type);
        }
    }

    /**
     * This is an overload of {@link #saveJson(Path, Object, Type)}, defaulting
     * to the Class of the provided object for the Type. This will only work for
     * simple types such as JsonObject, MyFancyJson. If you have a type with Generics
     * Such as a Map, you will want to use the other method and provide your type token as explained.
     *
     * @param path  The path to write the json to.
     * @param thing The Object we are serializing.
     * @throws IOException Thrown if there was an IO error.
     */
    public static void saveJson(Path path, Object thing) throws IOException {
        saveJson(path, thing, thing.getClass());
    }

    /**
     * Serializes an object to Json, storing it in provided Path.
     * <p>
     * Type can either be a static field using a Gson TypeToken for classes with Generic parameters as shown here:<br>
     * <code>Type type = new com.google.gson.reflect.TypeToken&lt;Map&lt;String,String&gt;&gt;(){}.getType();</code><br>
     * Or a Class instance such as: <code>JsonObject.class</code>
     *
     * @param path  The Path to write the json to.
     * @param thing The Object we are serializing.
     * @param type  The Type to Deserialize to.
     * @throws IOException Thrown if there was an IO error.
     */
    public static void saveJson(Path path, Object thing, Type type) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            GSON.toJson(thing, type, writer);
        }
    }

    /**
     * Helper method to get a field from a JsonObject using dot notation.
     */
    @Nullable
    public static JsonElement getField(JsonElement object, String dotNotation) {
        if (isNull(object)) return null;

        String[] keys = dotNotation.split("\\.");

        JsonElement currentElement = object;

        for (String key : keys) {
            if (currentElement.isJsonObject()) {
                JsonObject jsonObject = currentElement.getAsJsonObject();
                if (jsonObject.has(key)) {
                    currentElement = jsonObject.get(key);
                } else {
                    return null; // Key not found
                }
            } else {
                return null; // Not a JsonObject
            }
        }
        
        return currentElement.isJsonPrimitive() ? 
            currentElement.getAsJsonPrimitive() : null;
    }
    
    public static boolean isNull(@Nullable JsonElement element) {
        if (element == null) return true;
        return element.isJsonNull();
    }
}
