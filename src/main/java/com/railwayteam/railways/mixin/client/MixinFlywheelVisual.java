/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
 */

package com.railwayteam.railways.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.Railways;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Locale;

@Mixin(FlywheelVisual.class)
public class MixinFlywheelVisual {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"
        )
    )
    private Model railways$usePalettePartialForFlywheels(PartialModel partial, Operation<Model> original,
                                                         VisualizationContext context, FlywheelBlockEntity blockEntity, float partialTick) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        if (partial == AllPartialModels.FLYWHEEL
            && id != null
            && Railways.MOD_ID.equals(id.getNamespace())
            && id.getPath().endsWith("locometal_flywheel")) {
            String path = id.getPath();
            PalettesColor color;
            try {
                color = path.equals("locometal_flywheel")
                    ? PalettesColor.NETHERITE
                    : PalettesColor.valueOf(path.substring(0, path.length() - "_locometal_flywheel".length()).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                Railways.LOGGER.warn("Unknown palette flywheel block {} — falling back to block model", id);
                return Models.block(blockEntity.getBlockState());
            }

            PartialModel model = CRBlockPartials.FLYWHEELS.get(color);
            if (model != null) {
                return Models.partial(model);
            }

            Railways.LOGGER.warn("Missing palette flywheel partial for {} — falling back to block model", color.getSerializedName());
            return Models.block(blockEntity.getBlockState());
        }
        return original.call(partial);
    }
}
