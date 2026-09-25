package com.railwayteam.railways.content.animated_flywheel;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.config.CRConfigs;
import com.railwayteam.railways.content.palettes.PalettesColor;
import com.railwayteam.railways.mixin_interfaces.IDistanceTravelled;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Locale;

class FlywheelActorVisual extends ActorVisual {
	private static final double FLYWHEEL_DIAMETER = 2.8125; 

	private final RotatingInstance shaft;
	private final TransformedInstance wheel;
	private final Matrix4f baseTransform;

	private float angle;
	private float lastRenderTime;

	FlywheelActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext context) {
		super(visualizationContext, simulationWorld, context);

		BlockState state = context.state;
		BlockPos localPos = context.localPos;
		@SuppressWarnings("null")
		Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);

		Instancer<RotatingInstance> shaftInstancer = instancerProvider.instancer(
			AllInstanceTypes.ROTATING,
			Models.partial(AllPartialModels.SHAFT)
		);
		this.shaft = shaftInstancer.createInstance();
		this.shaft
			.setRotationAxis(axis)
			.setRotationOffset(KineticBlockEntityVisual.rotationOffset(state, axis, localPos))
			.setPosition(localPos)
			.light(localBlockLight(), 0)
			.setChanged();

		PartialModel wheelModel = getFlywheelModel(state);
		Instancer<TransformedInstance> wheelInstancer = instancerProvider.instancer(
			InstanceTypes.TRANSFORMED,
			Models.partial(wheelModel)
		);
		this.wheel = wheelInstancer.createInstance();

		@SuppressWarnings("null")
		Direction facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		this.wheel
			.setIdentityTransform()
			.translate(localPos)
			.center()
			.rotate(new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()));

		this.baseTransform = new Matrix4f(this.wheel.pose);
		this.lastRenderTime = Float.NaN;

		applyWheelAngle(0);
	}

	private PartialModel getFlywheelModel(BlockState state) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (id != null && Railways.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("locometal_flywheel")) {
			String path = id.getPath();
			PalettesColor color;
			try {
				color = path.equals("locometal_flywheel")
					? PalettesColor.NETHERITE
					: PalettesColor.valueOf(path.substring(0, path.length() - "_locometal_flywheel".length()).toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				Railways.LOGGER.warn("Unknown palette flywheel block {} — falling back to default model", id);
				return AllPartialModels.FLYWHEEL;
			}

			PartialModel model = CRBlockPartials.FLYWHEELS.get(color);
			if (model != null) {
				return model;
			}

			Railways.LOGGER.warn("Missing palette flywheel partial for {} — falling back to default model", color.getSerializedName());
		}
		return AllPartialModels.FLYWHEEL;
	}

	@Override
	public void beginFrame() {
		float renderTime = AnimationTickHolder.getRenderTime();
		if (Float.isNaN(lastRenderTime))
			lastRenderTime = renderTime;

		float deltaTicks = renderTime - lastRenderTime;
		lastRenderTime = renderTime;
		if (deltaTicks < 0)
			deltaTicks = 0;

		float speedMultiplier = CRConfigs.client().flywheelSpeedMultiplier.getF();
		float rpm = computeRpm() * speedMultiplier;
		float degreesPerTick = rpm * 360.0f / 1200.0f;
		this.angle = (this.angle + degreesPerTick * deltaTicks) % 360.0f;

		shaft.setRotationalSpeed(rpm).setChanged();
		applyWheelAngle(this.angle);
	}

	private float computeRpm() {
		if (!CRConfigs.client().animatedFlywheels.get())
			return 0;
		if (!(context.contraption instanceof CarriageContraption carriageContraption))
			return 0;
		if (!(carriageContraption.entity instanceof CarriageContraptionEntity carriageContraptionEntity))
			return 0;

		@SuppressWarnings("null")
		Direction.Axis axis = context.state.getValue(BlockStateProperties.AXIS);
		if (axis.isVertical())
			return 0;

		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		if (assemblyDirection.getAxis() == axis)
			return 0;

		// Signed distance moved this tick, in the carriage's own frame of travel,
		// so the spin direction does not depend on the train's world heading.
		double distancePerTick = ((IDistanceTravelled) carriageContraptionEntity).railways$getDistanceTravelled();
		double circumference = Math.PI * FLYWHEEL_DIAMETER;

		double rpm = (distancePerTick / circumference) * 1200.0;

		if (assemblyDirection == Direction.SOUTH || assemblyDirection == Direction.WEST)
			rpm = -rpm;
		if (!Double.isFinite(rpm))
			return 0;

		return (float) rpm;
	}

	private void applyWheelAngle(float angleDegrees) {
		wheel
			.setTransform(baseTransform)
			.rotateY(AngleHelper.rad(angleDegrees))
			.uncenter()
			.light(localBlockLight(), 0)
			.setChanged();
	}

	@Override
	protected void _delete() {
		shaft.delete();
		wheel.delete();
	}
}
