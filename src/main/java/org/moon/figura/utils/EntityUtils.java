package org.moon.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.moon.figura.mixin.ClientLevelInvoker;

import java.util.UUID;

public class EntityUtils {

    public static Entity getEntityByUUID(UUID uuid) {
        if (Minecraft.getInstance().level == null)
            return null;
        return ((ClientLevelInvoker) Minecraft.getInstance().level).getEntityGetter().get(uuid);
    }

    public static Entity getViewedEntity(float distance) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) return null;

        float tickDelta = Minecraft.getInstance().getFrameTime();
        Vec3 entityEye = entity.getEyePosition(tickDelta);
        Vec3 viewVec = entity.getViewVector(tickDelta).scale(distance);
        AABB box = entity.getBoundingBox().expandTowards(viewVec).inflate(1f, 1f, 1f);

        Vec3 raycastEnd = entityEye.add(viewVec);

        double raycastDistanceSquared; //Has to be squared for some reason, thanks minecraft for not making that clear
        BlockHitResult blockResult = entity.level.clip(new ClipContext(entityEye, raycastEnd, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        if (blockResult != null)
            raycastDistanceSquared = blockResult.getLocation().distanceToSqr(entityEye);
        else
            raycastDistanceSquared = distance * distance;

        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(entity, entityEye, raycastEnd, box, entity1 -> !entity1.isSpectator() && entity1.isPickable(), raycastDistanceSquared);
        if (entityHitResult != null)
            return entityHitResult.getEntity();
        return null;
    }

    public static String getNameForUUID(UUID uuid) {
        ClientPacketListener con = Minecraft.getInstance().getConnection();
        if (con != null) {
            PlayerInfo player = con.getPlayerInfo(uuid);
            if (player != null)
                return player.getProfile().getName();
        }

        Entity e = getEntityByUUID(uuid);
        if (e != null)
            return e.getName().getString();

        return null;
    }
}
