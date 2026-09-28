/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2026 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.railwayteam.railways.neoforge.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.railwayteam.railways.Railways;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.railwayteam.railways.compat.tracks.TrackCompatUtils.TRACK_COMPAT_MODS;

public class CompatTrackRecipeProvider implements DataProvider {
    private static final String SLEEPERS_TAG = "create:sleepers";


    private final PackOutput output;

    public CompatTrackRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> processRecipes(cache));
    }

    private void processRecipes(CachedOutput cache) {
        Path recipesPath = output.getOutputFolder(PackOutput.Target.DATA_PACK)
            .resolve(Railways.MOD_ID)
            .resolve("recipe")
            .resolve("sequenced_assembly");

        try {
            if (!Files.exists(recipesPath)) {
                return;
            }

            Files.list(recipesPath)
                .filter(path -> path.getFileName().toString().startsWith("track_")
                    && path.getFileName().toString().endsWith(".json"))
                .forEach(path -> processRecipeFile(path, cache));
        } catch (Exception e) {
            Railways.LOGGER.warn("Failed to process compat track recipe files", e);
        }
    }

    private void processRecipeFile(Path filePath, CachedOutput cache) {
        try {
            String fileName = filePath.getFileName().toString();
            String nameWithoutExt = fileName.substring(0, fileName.length() - 5);
            if (!nameWithoutExt.startsWith("track_")) {
                return;
            }

            String materialName = nameWithoutExt.substring("track_".length());
            if (materialName.endsWith("_wide")) {
                materialName = materialName.substring(0, materialName.length() - "_wide".length());
            } else if (materialName.endsWith("_narrow")) {
                materialName = materialName.substring(0, materialName.length() - "_narrow".length());
            }

            var track = CompatDataGenUtils.parseTrackName(materialName);
            if (track.isEmpty() || track.get().materialName().isBlank()) {
                return;
            }

            String compatPrefix = track.get().prefix();
            String compatModId = track.get().modId();
            String woodName = track.get().materialName();
            JsonObject json = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
            ResourceLocation trueSlabId = resolveCompatSlabId(compatPrefix, woodName);
            ResourceLocation generatedTagId = ResourceLocation.fromNamespaceAndPath(Railways.MOD_ID, "compat_slabs/" + compatModId + "/" + woodName);

            boolean changed = CompatDataGenUtils.addModLoadedCondition(json, compatModId);
            changed |= replaceTagsWithItem(json, trueSlabId);
            changed |= replaceItemsWithItem(json, trueSlabId);

            if (changed) {
                String modifiedContent = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
                Files.writeString(filePath, modifiedContent);
            }
        } catch (Exception e) {
            Railways.LOGGER.warn("Failed to process compat track recipe file: {}", filePath, e);
        }
    }

    private static boolean replaceTagsWithItem(JsonElement element, ResourceLocation slabId) {
        boolean changed = false;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("tag") && (SLEEPERS_TAG.equals(obj.get("tag").getAsString()) || obj.get("tag").getAsString().startsWith("railways:compat_slabs/"))) {
                obj.remove("tag");
                obj.addProperty("item", slabId.toString());
                changed = true;
            }

            List<String> keys = new ArrayList<>(obj.keySet());
            for (String key : keys) {
                changed |= replaceTagsWithItem(obj.get(key), slabId);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                changed |= replaceTagsWithItem(child, slabId);
            }
        }

        return changed;
    }

    private static boolean replaceItemsWithItem(JsonElement element, ResourceLocation slabId) {       
        boolean changed = false;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
                ResourceLocation itemId = ResourceLocation.tryParse(obj.get("item").getAsString());
                if (itemId != null && (itemId.equals(slabId) || (TRACK_COMPAT_MODS.contains(itemId.getNamespace()) && !BuiltInRegistries.ITEM.containsKey(itemId)))) {
                    // Just correctly re-set it
                    obj.addProperty("item", slabId.toString());
                    changed = true;
                }
            }

            List<String> keys = new ArrayList<>(obj.keySet());
            for (String key : keys) {
                changed |= replaceItemsWithItem(obj.get(key), slabId);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                changed |= replaceItemsWithItem(child, slabId);
            }
        }

        return changed;
    }

    private static ResourceLocation resolveCompatSlabId(String modId, String woodName) {
        return switch (modId) {
            case "twilightforest" -> ResourceLocation.fromNamespaceAndPath(modId, switch (woodName) {
                case "minewood" -> "mining_slab";
                case "transwood" -> "transformation_slab";
                default -> woodName.replace("wood", "") + "_slab";
            });
            case "quark" -> ResourceLocation.fromNamespaceAndPath(modId, switch (woodName) {
                case "blossom", "ancient", "azalea" -> woodName + "_planks_slab";
                default -> woodName + "_slab";
            });
            case "tfc" -> ResourceLocation.fromNamespaceAndPath(modId, "wood/planks/" + woodName + "_slab");
            case "hexcasting" -> ResourceLocation.fromNamespaceAndPath(modId, "edified_slab");
            default -> ResourceLocation.fromNamespaceAndPath(modId, woodName + "_slab");
        };
    }

    @Override
    public String getName() {
        return "Compat Track Recipe Post-Processor";
    }
}
