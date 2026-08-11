package nms.atmosphericfauna.particle.base;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class EnvironmentScanner {
    private final ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public EnvironmentScanner(ClientLevel level) {
        this.level = level;
    }

    //? if <=1.21.1 {
    // public int getLevelMinY() { return this.level.getMinBuildHeight(); }
    //?} else {
    public int getLevelMinY() {
        return this.level.getMinY();
    }
    //?}

    public boolean isBlocked(double px, double py, double pz) {
        return isBlocked(px, py, pz, null);
    }

    public boolean isBlocked(double px, double py, double pz, BlockPos exemptPos) {
        pos.set(px, py, pz);

        if (exemptPos != null && pos.equals(exemptPos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        if (state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES)) {
            return false;
        }

        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public boolean hasLineOfSight(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist <= 0.001)
            return true;

        int steps = (int) Math.ceil(dist * 2.0);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        double cx = x1;
        double cy = y1;
        double cz = z1;

        for (int i = 0; i < steps; i++) {
            cx += stepX;
            cy += stepY;
            cz += stepZ;
            if (isBlocked(cx, cy, cz, null)) {
                return false;
            }
        }
        return true;
    }

    public boolean isOceanBiome(double px, double pz) {
        pos.set(px, level.getSeaLevel(), pz);
        return level.getBiome(pos).is(BiomeTags.IS_OCEAN);
    }

    public double sampleGroundHeight(double px, double birdY, double pz) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(px), (int) Math.floor(pz));
        if (birdY >= surfaceY) return surfaceY;

        int startY = (int) Math.ceil(birdY);
        int endY = Math.max(getLevelMinY(), startY - 8);

        for (int y = startY; y >= endY; y--) {
            pos.set(px, y, pz);
            if (!level.isEmptyBlock(pos)) {
                return y + 1.0;
            }
        }
        return getLevelMinY();
    }
}
