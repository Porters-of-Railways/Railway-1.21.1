/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.railwayteam.railways.Railways;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Post-processes compat track loot tables to add mod_loaded conditions after Registrate generates them.
 */
public class CompatTrackLootTableProvider implements DataProvider {

    private static final Map<String, String> MOD_ID_MAP = Map.ofEntries(
        Map.entry("biomesoplenty", "biomesoplenty"),
        Map.entry("blue_skies", "blue_skies"),
        Map.entry("byg", "biomeswevegone"),
        Map.entry("create_dd", "create_dd"),
        Map.entry("hexcasting", "hexcasting"),
        Map.entry("natures_spirit", "natures_spirit"),
        Map.entry("quark", "quark"),
        Map.entry("tfc", "tfc"),
        Map.entry("twilightforest", "twilightforest")
    );

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public CompatTrackLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            // This runs after all other data providers
            // It will modify the generated loot table files to add mod_loaded conditions
            processLootTables();
        });
    }

    private void processLootTables() {
        Path lootTablesPath = output.getOutputFolder(PackOutput.Target.DATA_PACK)
            .resolve(Railways.MOD_ID)
            .resolve("loot_table")
            .resolve("blocks");
        
        try {
            if (java.nio.file.Files.exists(lootTablesPath)) {
                java.nio.file.Files.list(lootTablesPath)
                    .filter(path -> path.getFileName().toString().startsWith("track_") 
                            && path.getFileName().toString().endsWith(".json"))
                    .forEach(this::processLootTableFile);
            }
        } catch (Exception e) {
            Railways.LOGGER.warn("Failed to process compat track loot tables", e);
        }
    }

    private void processLootTableFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String nameWithoutExt = fileName.substring(0, fileName.length() - 5); // Remove .json
            String[] parts = nameWithoutExt.substring("track_".length()).split("_");
            
            if (parts.length > 0) {
                String modPrefix = parts[0];
                String modId = MOD_ID_MAP.get(modPrefix);
                
                if (modId != null) {
                    // Read the JSON
                    String content = new String(java.nio.file.Files.readAllBytes(filePath));
                    JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                    
                    // Check if conditions array already exists
                    if (!json.has("conditions")) {
                        json.add("conditions", new com.google.gson.JsonArray());
                    }
                    
                    com.google.gson.JsonArray conditions = json.getAsJsonArray("conditions");
                    
                    // Check if mod_loaded condition already exists
                    boolean hasModLoadedCondition = false;
                    for (JsonElement elem : conditions) {
                        if (elem.isJsonObject()) {
                            JsonObject cond = elem.getAsJsonObject();
                            if ("neoforge:mod_loaded".equals(cond.get("condition").getAsString()) 
                                && modId.equals(cond.get("modid").getAsString())) {
                                hasModLoadedCondition = true;
                                break;
                            }
                        }
                    }
                    
                    // Add the condition if it doesn't exist
                    if (!hasModLoadedCondition) {
                        JsonObject modLoadedCondition = new JsonObject();
                        modLoadedCondition.addProperty("condition", "neoforge:mod_loaded");
                        modLoadedCondition.addProperty("modid", modId);
                        conditions.add(modLoadedCondition);
                        
                        // Write the modified JSON back
                        String modifiedContent = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
                        java.nio.file.Files.write(filePath, modifiedContent.getBytes());
                    }
                }
            }
        } catch (Exception e) {
            Railways.LOGGER.warn("Failed to process loot table file: {}", filePath, e);
        }
    }

    @Override
    public String getName() {
        return "Compat Track Loot Table Post-Processor";
    }
}

