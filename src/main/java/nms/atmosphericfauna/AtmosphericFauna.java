package nms.atmosphericfauna;

import nms.atmosphericfauna.config.ConfigHandler;
import nms.atmosphericfauna.gui.DebugHudOverlay;
import nms.atmosphericfauna.particle.base.BaseBirdParticle;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.particle.BlueJayParticle;
import nms.atmosphericfauna.particle.CommonSwiftParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;
import nms.atmosphericfauna.command.ModCommands;
import static nms.atmosphericfauna.config.ConfigHandler.*;

import net.fabricmc.api.ClientModInitializer;
//? if <=1.21.11 {
/*import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
*//*?} else {*/
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
//?}
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtmosphericFauna implements ClientModInitializer {
	public static final String MOD_ID = "atmospheric-fauna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType BLUE_JAY = FabricParticleTypes.simple(true);
	public static final SimpleParticleType COMMON_SWIFT = FabricParticleTypes.simple(true);
	public static final SimpleParticleType CROW = FabricParticleTypes.simple(true);
	private static int chunkLoadCount = 0;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Client is initializing...");

		// Load configuration
		ConfigHandler.load();

		// Register particle types
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "blue_jay"),
				BLUE_JAY);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "common_swift"),
				COMMON_SWIFT);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "crow"), CROW);

		// Register particle factories
		//? if <=1.21.11 {
		/*
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.BLUE_JAY, BlueJayParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.COMMON_SWIFT,
				CommonSwiftParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
		*//*?} else {*/
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.BLUE_JAY, BlueJayParticle.Factory::new);
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.COMMON_SWIFT,
				CommonSwiftParticle.Factory::new);
		ParticleProviderRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
		//?}

		// Ambient spawning
		//? if <=1.21.11 {
		/*ClientTickEvents.END_WORLD_TICK.register(AmbientSpawning::tick);
		*//*?} else {*/
		ClientTickEvents.END_LEVEL_TICK.register(AmbientSpawning::tick);
		//?}
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			chunkLoadCount++;
			if (chunkLoadCount % 4 == 0 && enableChunkLoadSpawning) {
				int originalGlobalMax = maxActiveBirds;
				int originalBlueJayMax = maxActiveBlueJays;
				int originalCommonSwiftMax = maxActiveCommonSwifts;
				int originalCrowMax = maxActiveCrows;
				try {
					maxActiveBirds /= 2;
					maxActiveBlueJays /= 2;
					maxActiveCommonSwifts /= 2;
					maxActiveCrows /= 2;

					AmbientSpawning.runSpawnAttempt(world);
				} finally {
					maxActiveBirds = originalGlobalMax;
					maxActiveBlueJays = originalBlueJayMax;
					maxActiveCommonSwifts = originalCommonSwiftMax;
					maxActiveCrows = originalCrowMax;
				}
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			BaseBirdParticle.reset();
			if (ConfigHandler.enableDebugScreenOnJoin) {
				DebugHudOverlay.showDebug = true;
			}
		});

		// Register all client commands
		ModCommands.registerClientCommands();
		DebugHudOverlay.register();
	}
}
