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

package com.railwayteam.railways.util.packet;

import com.railwayteam.railways.multiloader.S2CPacket;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet that transfers the FULL state of a train (graph + travelling
 * points) via NBT, mirroring the 1.20 {@code TrainPacket}. {@code AddTrainPacket}
 * only carries {@link Train#STREAM_CODEC} data, which omits the track graph and
 * travelling points — without them the client cannot initialise carriage points
 * and a restored train is never rendered.
 */
public class FullTrainSyncPacket implements S2CPacket {
    private final CompoundTag trainNbt;
    private final DimensionPalette dimensions;

    public FullTrainSyncPacket(Train train, HolderLookup.Provider registries) {
        this.dimensions = new DimensionPalette();
        this.trainNbt = train.write(dimensions, registries);
    }

    public FullTrainSyncPacket(FriendlyByteBuf buf) {
        this.trainNbt = buf.readNbt();
        this.dimensions = DimensionPalette.receive(buf);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(trainNbt);
        dimensions.send(buffer);
    }

    @Override
    public void handle(Minecraft mc) {
        mc.execute(() -> {
            if (mc.level == null)
                return;
            Train train = Train.read(trainNbt, mc.level.registryAccess(),
                CreateClient.RAILWAYS.trackNetworks, dimensions);
            CreateClient.RAILWAYS.trains.put(train.id, train);
        });
    }
}
