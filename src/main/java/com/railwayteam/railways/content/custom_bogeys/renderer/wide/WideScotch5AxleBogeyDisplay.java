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

package com.railwayteam.railways.content.custom_bogeys.renderer.wide;

import com.railwayteam.railways.content.custom_bogeys.renderer.unified.BogeyDisplay;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.ElementProvider;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.transform.Affine;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import static com.railwayteam.railways.registry.CRBlockPartials.*;

// 5-axle wide scotch-yoke bogey: dedicated 5-axle frame + pistons models, and 5 wheel/pin axles along Z.
// Wide gauge (X) stays exact because it reuses the WIDE wheel/pin models and never offsets in X.
// The belt is baked into the frame model, so no separate (scrolling) belt partial is rendered here.
public class WideScotch5AxleBogeyDisplay implements BogeyDisplay {
    private static final int AXLES = 5;
    // Z spacing between axles (blocks). ~1.75 m (28px) matches a real 2-10-0 coupled-axle pitch
    // (e.g. DRB Class 50/52) and is baked into the 5-axle frame/piston models at the same value.
    private static final double AXLE_SPACING = 1.75;

    private final Affine<?> frame;
    private final Affine<?>[] wheels;
    private final Affine<?>[] pins;
    private final Affine<?> pistons;

    // Lengthwise driveshaft: N one-block SHAFT segments, same placement convention as
    // WideScotchYokeBogeyDisplay (i=0 at z=0, then z = -1, -2, ...). For even N that
    // centers the run at z = -0.5. Count chosen to span the long 5-axle frame.
    private static final int PRIMARY_SHAFT_COUNT = 10;
    private final Affine<?>[] primaryShafts = new Affine<?>[PRIMARY_SHAFT_COUNT];
    private final Affine<?>[] secondaryShafts = new Affine<?>[4];

    public WideScotch5AxleBogeyDisplay(ElementProvider<?> prov) {
        frame = prov.create(WIDE_SCOTCH_5AXLE_FRAME);
        wheels = prov.create(WIDE_SCOTCH_WHEELS, AXLES);
        pins = prov.create(WIDE_SCOTCH_PINS, AXLES);
        pistons = prov.create(WIDE_SCOTCH_5AXLE_PISTONS);
        prov.create(AllPartialModels.SHAFT, primaryShafts, secondaryShafts);
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle) {
        // z = (N/2 - 1) - i  → for N=2 gives {0,-1}; for N=10 gives {4..-5}
        int primaryZ0 = PRIMARY_SHAFT_COUNT / 2 - 1;
        for (int i = 0; i < PRIMARY_SHAFT_COUNT; i++) {
            primaryShafts[i]
                .translate(-.5, 4 / 16., primaryZ0 - i)
                .center()
                .rotateTo(Direction.UP, Direction.SOUTH)
                .rotateYDegrees(wheelAngle)
                .uncenter();
        }

        // Secondary (crosswise) shafts — same layout as single-axle WideScotchYoke, pushed to frame ends.
        double endZ = (AXLES - 1) / 2.0 * AXLE_SPACING; // 3.5
        for (int i : Iterate.zeroAndOne) {
            for (int side : Iterate.zeroAndOne) {
                secondaryShafts[i + (side * 2)]
                    .translate(-1 + side, 4 / 16., (i == 0 ? endZ : -endZ) + (10 / 16.) + i * -(36 / 16.))
                    .center()
                    .rotateTo(Direction.UP, Direction.EAST)
                    .rotateYDegrees(wheelAngle)
                    .uncenter();
            }
        }

        frame.self();

        pistons.translate(0, 1, 1 / 4f * Math.sin(AngleHelper.rad(wheelAngle)));

        // 5 wide axles centered on the bogey: only Y + Z offsets + X-axis spin.
        // No X translation -> the wide gauge baked into WIDE_SCOTCH_WHEELS/PINS is preserved.
        for (int i = 0; i < AXLES; i++) {
            double z = (i - (AXLES - 1) / 2.0) * AXLE_SPACING;

            wheels[i]
                .translate(0, 1, z)
                .rotateXDegrees(wheelAngle);

            pins[i]
                .translate(0, 1, z)
                .rotateXDegrees(wheelAngle)
                .translate(0, 1 / 4f, 0)
                .rotateXDegrees(-wheelAngle);
        }
    }
}