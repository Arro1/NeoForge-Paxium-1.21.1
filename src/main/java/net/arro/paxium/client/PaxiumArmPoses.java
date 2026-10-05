package net.arro.paxium.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

// Third-person arm poses added to HumanoidModel.ArmPose through META-INF/enumextensions.json.
public final class PaxiumArmPoses {
    // Channeling the Paxium Sword's fire beam. One-handed, so the off hand keeps its normal pose.
    public static final EnumProxy<HumanoidModel.ArmPose> FIRE_BEAM = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) PaxiumArmPoses::poseFireBeam);

    // Arm raised forward (~50°) and following the head, so the blade points ahead and slightly up
    // along the beam - replaces the vanilla SPEAR pose that lifts the sword straight overhead.
    // Same idea as vanilla's crossbow aim, but damped so it reads as presenting the blade.
    private static void poseFireBeam(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float inward = arm == HumanoidArm.RIGHT ? -0.15F : 0.15F;

        part.xRot = -0.9F + model.head.xRot * 0.6F;
        part.yRot = inward + model.head.yRot * 0.5F;
        part.zRot = 0.0F;
    }

    private PaxiumArmPoses() {
    }
}
