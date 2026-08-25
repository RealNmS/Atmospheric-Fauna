package nms.atmosphericfauna.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ProjectileMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void atmosphericfauna$onProjectileTick(CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        
        if (!projectile.level().isClientSide()) return;

        Vec3 movement = projectile.getDeltaMovement();
        
        if (movement.lengthSqr() > 0.01) {
            BaseBirdParticle.checkProjectileHit(
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    movement.x, movement.y, movement.z
            );
        }
    }
}
