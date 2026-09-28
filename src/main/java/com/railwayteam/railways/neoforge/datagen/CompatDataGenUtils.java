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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CompatDataGenUtils {
    private static final List<Map.Entry<String, String>> COMPAT_MODS = List.of(
        Map.entry("biomesoplenty", "biomesoplenty"),
        Map.entry("blue_skies", "blue_skies"),
        Map.entry("byg", "biomeswevegone"),
        Map.entry("create_dd", "create_dd"),
        Map.entry("hexcasting", "hexcasting"),
        Map.entry("natures_spirit", "natures_spirit"),
        Map.entry("quark", "quark"),
        Map.entry("tfc", "tfc"),
        Map.entry("twilightforest", "twilightforest")
    ).stream()
        .sorted(Map.Entry.<String, String>comparingByKey(
            Comparator.comparingInt(String::length).reversed()))
        .toList();

    private CompatDataGenUtils() {}

    public static Optional<CompatTrackName> parseTrackName(String trackName) {
        return COMPAT_MODS.stream()
            .filter(entry -> trackName.startsWith(entry.getKey() + "_"))
            .findFirst()
            .map(entry -> new CompatTrackName(
                entry.getKey(),
                entry.getValue(),
                trackName.substring(entry.getKey().length() + 1)
            ));
    }

    public static boolean addModLoadedCondition(JsonObject json, String modId) {
        JsonArray conditions;
        if (json.has("neoforge:conditions")) {
            JsonElement element = json.get("neoforge:conditions");
            if (!element.isJsonArray()) {
                throw new IllegalArgumentException("'neoforge:conditions' must be an array");
            }
            conditions = element.getAsJsonArray();
        } else {
            conditions = new JsonArray();
            json.add("neoforge:conditions", conditions);
        }

        for (JsonElement element : conditions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject condition = element.getAsJsonObject();
            if (hasString(condition, "type", "neoforge:mod_loaded")
                && hasString(condition, "modid", modId)) {
                return false;
            }
        }

        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", modId);
        conditions.add(condition);
        return true;
    }

    private static boolean hasString(JsonObject object, String key, String expected) {
        return object.has(key)
            && object.get(key).isJsonPrimitive()
            && object.get(key).getAsJsonPrimitive().isString()
            && expected.equals(object.get(key).getAsString());
    }

    public record CompatTrackName(String prefix, String modId, String materialName) {}
}
