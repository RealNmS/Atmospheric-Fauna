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
            if (client.player != null && client.player.isScoping()) {
                if (client.level.getGameTime() % 10 == 0) {
                    BaseBirdParticle lookedAtBird = BaseBirdParticle.getBirdInCrosshairs(client.player);
                    if (lookedAtBird != null) {
                        LOGGER.info("Spyglass focused on: " + lookedAtBird.getBaseSpriteName());
                    }
                }
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
