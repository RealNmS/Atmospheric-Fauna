package nms.atmosphericfauna.particle.base;

import net.minecraft.client.Minecraft;

public class BirdAnimator {
    private final BaseBirdParticle bird;
    private String spriteName = null;
    private boolean facingRight = false;

    public BirdAnimator(BaseBirdParticle bird) {
        this.bird = bird;
    }

    public void updateSprite(int frame, boolean isPerched) {
        if (bird.getBaseSpriteName() == null) {
            return;
        }

        StringBuilder builder = new StringBuilder(bird.getBaseSpriteName());
        builder.append(isPerched ? "_perched" : "_flying");
        if (this.facingRight) builder.append("_r");
        builder.append("_").append(frame);

        this.spriteName = builder.toString();
        
        bird.applySprite(this.spriteName); 
    }

    // FIX: Require the physics data to be passed in, rather than reaching for it
    public void updateFacingDirection(boolean isPerched, double x, double z, double xd, double zd) {
        this.facingRight = shouldFaceRightRelativeToCamera(x, z, xd, zd);
        updateSprite(getCurrentFrame(), isPerched);
    }

    public int getCurrentFrame() {
        if (this.spriteName == null || this.spriteName.isEmpty()) return 1;
        
        char lastChar = this.spriteName.charAt(this.spriteName.length() - 1);
        if (Character.isDigit(lastChar)) {
            return Character.getNumericValue(lastChar);
        }
        return 1;
    }

    // FIX: Using the passed arguments instead of bird.x and bird.z
    private boolean shouldFaceRightRelativeToCamera(double x, double z, double xd, double zd) {
        double horizSpeedSq = xd * xd + zd * zd;
        if (horizSpeedSq <= 0.0001) return this.facingRight;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return this.facingRight;

        double dx = x - mc.player.getX();
        double dz = z - mc.player.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq < 0.0001) return this.facingRight;

        double dist = Math.sqrt(distSq);
        double perpendicularSpeed = (zd * (dx / dist)) - (xd * (dz / dist));

        if (Math.abs(perpendicularSpeed) < 0.01) return this.facingRight;

        return perpendicularSpeed > 0.0;
    }
}
