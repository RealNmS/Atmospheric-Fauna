package nms.atmosphericfauna.mixin;

import nms.atmosphericfauna.particle.base.BaseBirdParticle;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if <=1.21.5 {
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    
    //? if <=1.21.5 {
    // @Inject(method = "play", at = @At("RETURN"))
    // private void atmosphericfauna$onSoundPlayed(SoundInstance sound, CallbackInfo ci) {
    //?} else {
    @Inject(method = "play", at = @At("RETURN"))
    private void atmosphericfauna$onSoundPlayed(SoundInstance sound, CallbackInfoReturnable<?> cir) {
    //?}
        if (sound == null) return;
        
        SoundSource source = sound.getSource();
        
        if (source == SoundSource.PLAYERS || source == SoundSource.BLOCKS || source == SoundSource.HOSTILE) {
            float volume = 1.0f;
            
            try {
                volume = sound.getVolume();
            } catch (Exception e) {
                // Ignore exceptions when getting volume
            }

            String soundId = sound.getIdentifier().getPath();
            if (soundId.contains("explode")) {
                BaseBirdParticle.onExplosion(sound.getX(), sound.getY(), sound.getZ(), volume);
            }

            BaseBirdParticle.onSoundPlayed(sound.getX(), sound.getY(), sound.getZ(), volume);
        }
    }
}
