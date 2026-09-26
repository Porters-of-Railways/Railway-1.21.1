/*
 * Steam 'n' Rails
 * Copyright (c) 2026 The Railways Team
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

package com.railwayteam.railways.mixin_interfaces;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IShadowTrain {
    void railways$setShadow(@NotNull ResourceLocation shadowKey);
    void railways$clearShadow();
    @Nullable ResourceLocation railways$getShadowKey();
    void railways$setShadowSnapshot(@Nullable CompoundTag snapshot, @Nullable DimensionPalette dimensions);
    @Nullable CompoundTag railways$getShadowSnapshot();
    @Nullable DimensionPalette railways$getShadowSnapshotDimensions();
    default boolean railways$isShadow() {
        return railways$getShadowKey() != null;
    }
}
