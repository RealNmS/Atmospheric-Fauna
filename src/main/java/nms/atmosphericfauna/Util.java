package nms.atmosphericfauna;

import org.jspecify.annotations.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

public class Util {
    public static Identifier getId(@NonNull String path) {
        return Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, path);
    }

    public static TextureAtlasSprite getSprite(@NonNull String path) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(getId(path));
    }
}
