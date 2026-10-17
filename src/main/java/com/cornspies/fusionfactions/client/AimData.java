package com.cornspies.fusionfactions.client;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.cornspies.fusionfactions.FusionFactions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

// Where each weapon sits in first person while aiming down sights (D38): the hand offset (y, z in blocks)
// that puts its aiming line on the middle of the screen, for the iron sights (0) and each sight tier (1-3).
// Computed by tools/weapon_models.py from the models and written to assets/fusionfactions/weapon_aim.json.
public final class AimData implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier FILE = FusionFactions.id("weapon_aim.json");
    // Used for a weapon without data: roughly in front of the eye, a bit low.
    private static final float[] FALLBACK = { -0.25F, -0.4F, 0.0F };

    private static Map<Item, float[][]> offsets = Map.of();

    public static void register(AddClientReloadListenersEvent event) {
        event.addListener(FusionFactions.id("weapon_aim"), new AimData());
    }

    public static float[] offset(Item weapon, int sightTier) {
        float[][] forWeapon = offsets.get(weapon);
        return forWeapon != null && forWeapon[sightTier] != null ? forWeapon[sightTier] : FALLBACK;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Item, float[][]> loaded = new HashMap<>();
        Resource resource = resourceManager.getResource(FILE).orElse(null);
        if (resource == null) {
            LOGGER.warn("Missing {}; weapons will aim from a default position", FILE);
        } else {
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> weapon : root.entrySet()) {
                    Item item = BuiltInRegistries.ITEM.getValue(FusionFactions.id(weapon.getKey()));
                    float[][] tiers = new float[4][];
                    for (Map.Entry<String, JsonElement> tier : weapon.getValue().getAsJsonObject().entrySet()) {
                        // Height and depth, and a sideways offset for weapons whose sights aren't on their middle.
                        var values = tier.getValue().getAsJsonArray();
                        tiers[Integer.parseInt(tier.getKey())] = new float[] { values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                                values.size() > 2 ? values.get(2).getAsFloat() : 0.0F };
                    }
                    loaded.put(item, tiers);
                }
            } catch (Exception e) {
                LOGGER.error("Could not read {}", FILE, e);
            }
        }
        offsets = loaded;
    }
}
