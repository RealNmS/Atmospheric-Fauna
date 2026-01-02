package nms.atmosphericfauna;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtmosphericFauna implements ModInitializer {
	public static final String MOD_ID = "atmospheric-fauna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final SimpleParticleType CROW = FabricParticleTypes.simple();

	@Override
	public void onInitialize() {
		LOGGER.info("Atmospheric Fauna is taking flight!");
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "crow"), CROW);
	}
}
