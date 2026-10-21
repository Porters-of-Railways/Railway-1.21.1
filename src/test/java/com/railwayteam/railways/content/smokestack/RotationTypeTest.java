package com.railwayteam.railways.content.smokestack;

import com.railwayteam.railways.registry.CRBlocks;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationTypeTest {
    @Test
    void rotatesSmokestackHorizontalAxisForEveryRotation() {
        Block block = CRBlocks.CABOOSESTYLE_STACK.get();
        for (Axis axis : new Axis[]{Axis.X, Axis.Z}) {
            BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
            for (Rotation rotation : Rotation.values()) {
                Axis expected = switch (rotation) {
                    case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> axis == Axis.X ? Axis.Z : Axis.X;
                    default -> axis;
                };
                BlockState rotated = RotationType.AXIS.rotate(state, rotation);
                assertEquals(expected, rotated.getValue(BlockStateProperties.HORIZONTAL_AXIS));
            }
        }
    }

}
