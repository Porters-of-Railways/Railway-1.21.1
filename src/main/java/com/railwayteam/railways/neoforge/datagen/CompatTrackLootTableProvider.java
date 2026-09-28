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

import com.google.gson.JsonObject;
import com.railwayteam.railways.Railways;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Post-processes compat track loot tables to add mod_loaded conditions after Registrate generates them.
 */
public class CompatTrackLootTableProvider implements DataProvider {

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
            String nameWithoutExt = fileName.substring(0, fileName.length() - 5);
            if (!nameWithoutExt.startsWith("track_")) {
                return;
            }

            var track = CompatDataGenUtils.parseTrackName(
                nameWithoutExt.substring("track_".length()));
            if (track.isEmpty()) {
                return;
            }

            String content = new String(java.nio.file.Files.readAllBytes(filePath));
            JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if (CompatDataGenUtils.addModLoadedCondition(json, track.get().modId())) {
                String modifiedContent = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
                java.nio.file.Files.write(filePath, modifiedContent.getBytes());
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

