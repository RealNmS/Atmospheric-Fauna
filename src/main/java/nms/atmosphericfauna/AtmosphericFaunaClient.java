package nms.atmosphericfauna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import nms.atmosphericfauna.particle.CrowParticle;

public class AtmosphericFaunaClient implements ClientModInitializer {

    @SuppressWarnings("null")
    @Override
    public void onInitializeClient() {
        AtmosphericFauna.LOGGER.info("Registering Mod Particle Factories for " + AtmosphericFauna.MOD_ID);
        ParticleFactoryRegistry.getInstance().register(AtmosphericFauna.CROW, CrowParticle.Factory::new);
    }
}
