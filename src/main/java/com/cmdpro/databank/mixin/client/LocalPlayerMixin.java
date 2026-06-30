package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.worldgui.WorldGuiEntity;
import com.cmdpro.databank.worldgui.WorldGuiHitResult;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private static void Databank$pick(Entity entity, double blockInteractionRange, double entityInteractionRange, float partialTick, CallbackInfoReturnable<HitResult> cir) {
        HitResult result = cir.getReturnValue();

        Vec3 vec3 = entity.getEyePosition(partialTick);

        double range = Math.min(entityInteractionRange, result.getLocation().distanceTo(vec3));

        Vec3 vec31 = entity.getViewVector(partialTick);
        Vec3 vec32 = vec3.add(vec31.x() * range, vec31.y() * range, vec31.z() * range);

        Level level = entity.level();
        List<WorldGuiEntity> guis = level.getEntitiesOfClass(
            WorldGuiEntity.class,
            AABB.ofSize(entity.position(), 24, 24, 24)
        );
        double closestDistance = -1;
        WorldGuiEntity closest = null;
        WorldGuiEntity.WorldGuiIntersectionResult closestResult = null;
        for (WorldGuiEntity i : guis) {
            if (i.getGuiType() == null) {
                continue;
            }

            WorldGuiEntity.WorldGuiIntersectionResult intersectionResult = i.getLineIntersectResult(vec3, vec32);
            if (intersectionResult != null) {
                Vec3 pos = intersectionResult.pos;
                double distance = vec3.distanceTo(pos);
                if (closestDistance < 0 || distance < closestDistance) {
                    closestDistance = distance;
                    closest = i;
                    closestResult = intersectionResult;
                }
            }
        }
        if (closest != null) {
            double distance = vec3.distanceTo(closestResult.pos);
            if (distance < vec3.distanceTo(result.getLocation())) {
                WorldGuiHitResult hitResult = new WorldGuiHitResult(closest, closestResult.pos, closestResult);
                cir.setReturnValue(hitResult);
            }
        }
    }
}
