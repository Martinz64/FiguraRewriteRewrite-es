package org.moon.figura.ducks;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;

import java.util.UUID;

public interface ParticleEngineAccessor {

    <T extends ParticleOptions> Particle figura$makeParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ);
    void figura$spawnParticle(Particle particle, UUID owner);
    void figura$clearParticles(UUID owner);
    void figura$clearAllParticles();
}
