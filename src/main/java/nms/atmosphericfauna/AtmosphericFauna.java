package nms.atmosphericfauna;

import nms.atmosphericfauna.config.ConfigHandler;
import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import nms.atmosphericfauna.particle.BlueJayParticle;
import nms.atmosphericfauna.particle.CommonSwiftParticle;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.particle.NorthernCardinalParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;
import nms.atmosphericfauna.command.ModCommands;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.fabricmc.api.ClientModInitializer;
//? if <=1.21.11 {
// import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
//?} else {
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
//?}
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
//? if <=1.21.5 {
//  import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?} else {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class AtmosphericFauna implements ClientModInitializer {
	public static final String MOD_ID = "atmospheric-fauna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType BLUE_JAY = FabricParticleTypes.simple(true);
	public static final SimpleParticleType COMMON_SWIFT = FabricParticleTypes.simple(true);
	public static final SimpleParticleType CROW = FabricParticleTypes.simple(true);
	public static final SimpleParticleType NORTHERN_CARDINAL = FabricParticleTypes.simple(true);
	private static int chunkLoadCount = 0;
    private static ClientLevel lastLevel = null;

	@Override
	public void onInitializeClient() {
        LOGGER.info("Atmospheric Fauna is initializing...");

        LOGGER.debug("loading configuration...");
		ConfigHandler.load();

        LOGGER.debug("registering particle types...");
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "blue_jay"),
				BLUE_JAY);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "common_swift"),
				COMMON_SWIFT);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "crow"), CROW);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "northern_cardinal"),
				NORTHERN_CARDINAL);

        LOGGER.debug("registering particle factories...");
		//? if <=1.21.11 {
		/*
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.BLUE_JAY, BlueJayParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.COMMON_SWIFT,
				CommonSwiftParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.NORTHERN_CARDINAL, NorthernCardinalParticle.Factory::new);
		*//*?} else {*/
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.BLUE_JAY, BlueJayParticle.Factory::new);
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.COMMON_SWIFT,
				CommonSwiftParticle.Factory::new);
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.NORTHERN_CARDINAL, NorthernCardinalParticle.Factory::new);
		//?}

        LOGGER.debug("setting up ambient spawning...");
		//? if <=1.21.11 {
		// ClientTickEvents.END_WORLD_TICK.register(AmbientSpawning::tick);
		//?} else {
		ClientTickEvents.END_LEVEL_TICK.register(AmbientSpawning::tick);
		//?}
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.level != lastLevel) {
                BaseBirdParticle.reset();
                lastLevel = client.level;
            }
        });
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world != lastLevel) {
                BaseBirdParticle.reset();
                lastLevel = world;
            }
			chunkLoadCount++;
			if (chunkLoadCount % 4 == 0 && enableChunkLoadSpawning) {
                Map<Field, Integer> originalValues = new HashMap<>();
				try {
                    for (Field field : ConfigHandler.class.getDeclaredFields()) {
                        if (field.getType() == int.class && field.getName().startsWith("maxActive")) {
                            originalValues.put(field, field.getInt(null));
                            field.setInt(null, field.getInt(null) / 2);
                        }
                    }
					AmbientSpawning.runSpawnAttempt(world);
                } catch (Exception e) {
                    AtmosphericFauna.LOGGER.error("Failed to dynamically adjust bird caps for chunk load spawning", e);
				} finally {
                    for (Map.Entry<Field, Integer> entry : originalValues.entrySet()) {
                        try {
                            entry.getKey().setInt(null, entry.getValue());
                        } catch (Exception e) {
                            AtmosphericFauna.LOGGER.error("Failed to restore bird caps", e);
                        }
                    }
				}
			}
		});

        LOGGER.debug("registering client tick events...");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null)
                return;

            // Spyglass
            if (!enableSpyglassInteraction) {
                BaseBirdParticle.hoveredBird = null;
            } else if (client.player != null && client.player.isScoping()) {
                BaseBirdParticle.hoveredBird = BaseBirdParticle.getBirdInCrosshairs(client.player);
            } else {
                BaseBirdParticle.hoveredBird = null;
            }

            // Check for projectile hits
            try {
                for (var entity : client.level.entitiesForRendering()) {
                    if (!(entity instanceof Projectile projectile))
                        continue;

                    Vec3 movement = projectile.getDeltaMovement();
                    if (movement.lengthSqr() > 0.01) {
                        BaseBirdParticle.checkProjectileHit(
                                projectile.getX(), projectile.getY(), projectile.getZ(),
                                movement.x, movement.y, movement.z);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error checking for projectile hits", e);
            }
        });

        LOGGER.debug("registering spyglass HUD overlay...");
        //? if <=1.21.5 {
        // net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
        //?} else {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AtmosphericFauna.MOD_ID, "spyglass_hud"),
                (graphics, tickDelta) -> {
        //?}
                    if (!enableSpyglassInteraction) {
                        return;
                    }

            BaseBirdParticle bird = BaseBirdParticle.hoveredBird;

            if (bird != null) {
                Minecraft client = Minecraft.getInstance();

                int x = 4;
                int y = 4;

                String displayName = bird.getBaseSpriteName().replace("_", " ").toUpperCase();

                int textWidth = client.font.width(displayName);

                // Total Width = 4 (left padding) + 32 (sprite) + 8 (mid padding) + textWidth +
                // 8 (right padding)
                int totalWidth = 52 + textWidth;
                int totalHeight = 40; // 4 (top) + 32 (sprite) + 4 (bottom)

                int spriteX = x + 4;
                int spriteY = y + 4;

                int textX = spriteX + 32 + 8;
                int textY = y + 16;

                Identifier bgMain = Identifier.withDefaultNamespace("toast/advancement");
                Identifier bgSprite = Identifier.withDefaultNamespace("toast/recipe");
                Identifier texture = Identifier.fromNamespaceAndPath(MOD_ID,
                        "textures/particle/" + bird.getBaseSpriteName() + "_perched_1.png");

                //? if <=1.21.1 {
                // com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                // graphics.blitSprite(bgMain, x, y, totalWidth, totalHeight);
                // graphics.blitSprite(bgSprite, spriteX, spriteY, 32, 32);
                // graphics.blit(texture, spriteX, spriteY, 0, 0, 32, 32, 32, 32);
                // com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                //?} else if <=1.21.5 {
                // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                // bgMain, x, y, totalWidth, totalHeight);
                // graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                // bgSprite, spriteX, spriteY, 32, 32);
                // graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture,
                // spriteX, spriteY, 0.0F, 0.0F, 32, 32, 32, 32);
                //?} else {
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgMain, x, y,
                        totalWidth, totalHeight);
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, bgSprite,
                        spriteX, spriteY, 32, 32);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, spriteX,
                        spriteY, 0.0F, 0.0F, 32, 32, 32, 32);
                //?}

                //? if <=1.21.11 {
                // graphics.drawString(client.font, displayName, textX, textY, -1, true);
                //?} else {
                graphics.text(client.font, displayName, textX, textY, -1, true);
                //?}
            }
        });

        LOGGER.debug("loading client commands...");
        ModCommands.registerClientCommands();

        LOGGER.debug("registering debug HUD overlay...");
        DebugHudOverlay.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (ConfigHandler.enableDebugScreenOnJoin) {
				DebugHudOverlay.showDebug = true;
			}
        });
	}
}
