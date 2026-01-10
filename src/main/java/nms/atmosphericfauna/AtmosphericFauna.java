package nms.atmosphericfauna;

import nms.atmosphericfauna.config.ConfigHandler;
import nms.atmosphericfauna.particle.CrowParticle;
import nms.atmosphericfauna.spawning.AmbientSpawning;

import net.fabricmc.api.ClientModInitializer;
// import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
// import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtmosphericFauna implements /* ModInitializer, */ ClientModInitializer {
	public static final String MOD_ID = "atmospheric-fauna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType CROW = FabricParticleTypes.simple(true);

	private static int chunkLoadCount = 0;

	/*
	 * only for now because idk how to set up client and server initializers
	 * properly
	 * 
	 * @Override
	 * public void onInitialize() {
	 * LOGGER.info("Atmospheric Fauna is taking flight!");
	 * ConfigHandler.load();
	 * Registry.register(BuiltInRegistries.PARTICLE_TYPE,
	 * Identifier.fromNamespaceAndPath(MOD_ID, "crow"), CROW);
	 * ServerTickEvents.END_WORLD_TICK.register(AmbientSpawning::tick);
	 * }
	 */

	@Override
	public void onInitializeClient() {
		LOGGER.info("[Atmospheric Fauna] Client is initializing...");

		// Load configuration

		ConfigHandler.load();

		// Register particle types

		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "crow"), CROW);

		// Register particle factories

		ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);

		// Ambient spawning

		ClientTickEvents.END_WORLD_TICK.register(AmbientSpawning::tick);

		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			chunkLoadCount++;
			if (chunkLoadCount % 4 == 0) {
				AmbientSpawning.runSpawnAttempt(world);
			}
		});
	}
}
