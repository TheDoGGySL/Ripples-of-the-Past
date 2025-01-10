package com.github.standobyte.jojo.mixin;

import com.github.standobyte.jojo.capability.world.TimeStopHandler;
import com.github.standobyte.jojo.entity.itemprojectile.ItemProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends Entity {
    @Shadow public abstract void shoot(double  $$1, double  $$2, double  $$3, float  $$4, float  $$5);
    @Unique
    public float ripples_of_the_Past$tsFlightTicks = 5;
    public ProjectileEntityMixin(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
    }

    @Inject(method = "shootFromRotation", at = @At(value = "HEAD"), cancellable = true)
    public void jojoShootFromRotation(Entity entity, float $$1, float $$2, float $$3, float $$4, float $$5, CallbackInfo ci) {
        if ((((ProjectileEntity) (Object) this) instanceof ItemProjectileEntity) && TimeStopHandler.isTimeStopped(level, this.blockPosition())) {
            double $$6 = -Math.sin($$2 * (Math.PI / 180.0)) * Math.cos($$1 *  (Math.PI / 180.0));
            double $$7 = -Math.sin($$1 * (Math.PI / 180.0));
            double $$8 = Math.cos($$2 * (Math.PI / 180.0)) * Math.cos($$1 *  (Math.PI / 180.0));
            this.shoot($$6, $$7, $$8, $$4, $$5);
            Vector3d $$9 = entity.getDeltaMovement();
            this.setDeltaMovement(this.getDeltaMovement().add($$9.x, entity.isOnGround() ? 0.0 : $$9.y, $$9.z));
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void projectileTimestoptick(CallbackInfo ci) {
        boolean timeStop = TimeStopHandler.isTimeStopped(level, this.blockPosition());
        if (timeStop) {
            if (ripples_of_the_Past$tsFlightTicks > 0) {
                ripples_of_the_Past$tsFlightTicks--;
                super.canUpdate(true);
            }
            else {
                super.canUpdate(false);
            }
        }
        else {
            ripples_of_the_Past$tsFlightTicks = 5;}
    }
}