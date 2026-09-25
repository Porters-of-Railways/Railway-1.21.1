package com.railwayteam.railways.neoforge.datagen;

import com.railwayteam.railways.Railways;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.railwayteam.railways.compat.tracks.TrackCompatUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CompatTrackTagProvider extends TagsProvider<Item> {
    private static final Map<String, String> RENAMED_COMPAT_NAMESPACES = Map.of(
        "byg", "biomeswevegone"
    );

    public CompatTrackTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, Registries.ITEM, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (String modId : new ArrayList<>(TrackCompatUtils.TRACK_COMPAT_MODS)) {
            for (TrackMaterial material : new ArrayList<>(TrackMaterial.allFromMod(modId))) {
                ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath("railways", "compat_slabs/" + material.id.getNamespace() + "/" + material.resourceName());
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

                String woodName = material.resourceName().replace("_narrow", "").replace("_wide", "");
                String namespace = RENAMED_COMPAT_NAMESPACES.getOrDefault(material.id.getNamespace(), material.id.getNamespace());
                ResourceLocation actualCompatSlabId = resolveCompatSlabId(namespace, woodName);
                this.tag(tagKey).addOptional(actualCompatSlabId);
            }
        }
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
}