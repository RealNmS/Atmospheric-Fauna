package nms.atmosphericfauna;

import nms.atmosphericfauna.particle.CrowParticle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class AtmosphericFaunaClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AtmosphericFauna.LOGGER.info("Registering Mod Particle Factories for " + AtmosphericFauna.MOD_ID);
        ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
    }
}
