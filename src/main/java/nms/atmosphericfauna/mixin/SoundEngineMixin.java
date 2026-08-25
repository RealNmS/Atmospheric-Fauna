package nms.atmosphericfauna.mixin;

import nms.atmosphericfauna.particle.base.BaseBirdParticle;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    
    @Inject(method = "play", at = @At("RETURN"))
    private void atmosphericfauna$onSoundPlayed(SoundInstance sound, CallbackInfoReturnable<?> cir) {
        if (sound == null) return;
        
        SoundSource source = sound.getSource();
        
        if (source == SoundSource.PLAYERS || source == SoundSource.BLOCKS || source == SoundSource.HOSTILE) {
            float volume = 1.0f;
            
            try {
                volume = sound.getVolume();
            } catch (Exception e) {
                // Ignore exceptions when getting volume
            }
            
            BaseBirdParticle.onSoundPlayed(sound.getX(), sound.getY(), sound.getZ(), volume);
        }
    }
}
