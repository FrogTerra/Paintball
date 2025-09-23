package me.FrogTerra.paintball.utility;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;

/**
 * Custom Gson adapter for serializing and deserializing Bukkit Location objects
 */
public class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {

    @Override
    public JsonElement serialize(Location location, Type type, JsonSerializationContext context) {
        if (location == null) {
            return JsonNull.INSTANCE;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("world", location.getWorld() != null ? location.getWorld().getName() : "world");
        jsonObject.addProperty("x", location.getX());
        jsonObject.addProperty("y", location.getY());
        jsonObject.addProperty("z", location.getZ());
        jsonObject.addProperty("yaw", location.getYaw());
        jsonObject.addProperty("pitch", location.getPitch());

        return jsonObject;
    }

    @Override
    public Location deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement.isJsonNull()) {
            return null;
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String worldName = jsonObject.get("world").getAsString();
        World world = Bukkit.getWorld(worldName);

        // If world doesn't exist, use the first available world or create a dummy location
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }

        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        double z = jsonObject.get("z").getAsDouble();
        float yaw = jsonObject.has("yaw") ? jsonObject.get("yaw").getAsFloat() : 0.0f;
        float pitch = jsonObject.has("pitch") ? jsonObject.get("pitch").getAsFloat() : 0.0f;

        return new Location(world, x, y, z, yaw, pitch);
    }
}