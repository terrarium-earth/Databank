package com.cmdpro.databank.worldgui;

import com.cmdpro.databank.registry.EntityDataSerializerRegistry;
import com.cmdpro.databank.registry.EntityRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class WorldGuiEntity extends Entity {
    public static final EntityDataAccessor<Optional<WorldGui>> GUI_DATA = SynchedEntityData.defineId(
        WorldGuiEntity.class,
        EntityDataSerializerRegistry.WORLD_GUI.get()
    );

    public WorldGuiEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public WorldGuiEntity(Level level, Vec3 position, WorldGui data) {
        this(EntityRegistry.WORLD_GUI.get(), level);
        setPos(position);
        setGuiData(data);
    }

    @Nullable
    public WorldGui getGuiData() {
        return getEntityData().get(GUI_DATA).orElse(null);
    }

    @Nullable
    public WorldGuiType getGuiType() {
        var gui = getGuiData();
        return gui == null ? null : gui.getType();
    }

    private void setGuiData(@Nullable WorldGui data) {
        getEntityData().set(GUI_DATA, Optional.ofNullable(data));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GUI_DATA, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        valueInput.read("GUI", WorldGui.CODEC).ifPresent(this::setGuiData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        valueOutput.store("GUI", WorldGui.CODEC, getGuiData());
    }

    public Vec3 getBoundsCorner(float multX, float multY) {
        Vec2 size = getGuiType().getMenuWorldSize(this);
        Vector3f vec3 = new Vector3f((size.x / 2f) * multX, 0f, (size.y / 2f) * multY);
        Matrix3f matrix = new Matrix3f();
        List<Matrix3f> matrixs = getGuiData().getMatrixs();
        for (Matrix3f i : matrixs) {
            matrix.mul(i);
        }
        matrix.transform(vec3);
        Vec3 corner = position().add(vec3.x(), vec3.y(), vec3.z());
        return corner;
    }

    @Override
    public boolean shouldBeSaved() {
        var guiType = getGuiType();

        return (guiType == null || guiType.saves()) && super.shouldBeSaved();
    }

    @Override
    public void tick() {
        super.tick();
        WorldGui gui = getGuiData();
        if (gui != null) {
            gui.tick();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    public WorldGuiIntersectionResult getLineIntersectResult(Vec3 lineStart, Vec3 lineEnd) {
        Vector3f topLeft = getBoundsCorner(-1, 1).toVector3f();
        Vector3f topRight = getBoundsCorner(1, 1).toVector3f();
        Vector3f bottomLeft = getBoundsCorner(-1, -1).toVector3f();
        Vector3f bottomRight = getBoundsCorner(1, -1).toVector3f();
        Vector3f start = lineStart.toVector3f();
        WorldGuiIntersectionResult result1 = intersectTriangle(
            start,
            lineStart.vectorTo(lineEnd).toVector3f(),
            (float) lineStart.distanceTo(lineEnd),
            topLeft,
            topRight,
            bottomLeft
        );
        if (result1 != null) {
            result1 = new WorldGuiIntersectionResult(new Vec2(1f - result1.normal.x, result1.normal.y), result1.pos);
            if (result1.normal.length() >= 1) {
                result1.normal = result1.normal.normalized();
            }
            return result1;
        }
        WorldGuiIntersectionResult result2 = intersectTriangle(
            start,
            lineStart.vectorTo(lineEnd).toVector3f(),
            (float) lineStart.distanceTo(lineEnd),
            bottomRight,
            bottomLeft,
            topRight
        );
        if (result2 != null) {
            result2 = new WorldGuiIntersectionResult(new Vec2(result2.normal.x, 1f - result2.normal.y), result2.pos);
            if (result2.normal.length() >= 1) {
                result2.normal = result2.normal.normalized();
            }
            return result2;
        }
        return null;
    }

    private WorldGuiIntersectionResult intersectTriangle(Vector3f rayStart, Vector3f direction, float maxDistance, Vector3f triangle1, Vector3f triangle2, Vector3f triangle3) {
        Vector3f e1 = new Vector3f(triangle2).sub(triangle1);
        Vector3f e2 = new Vector3f(triangle3).sub(triangle1);

        Vector3f h = new Vector3f(direction).cross(e2);
        float a = e1.dot(h);

        if (a > -0.00001f && a < 0.00001f) {
            return null;
        }

        float f = 1f / a;
        Vector3f s = new Vector3f(rayStart).sub(triangle1);
        float u = f * (s.dot(h));

        if (u < 0.0f || u > 1.0f) {
            return null;
        }

        Vector3f q = new Vector3f(s).cross(e1);
        float v = f * direction.dot(q);

        if (v < 0.0f || u + v > 1.0f) {
            return null;
        }

        float t = f * e2.dot(q);

        Vector3f hitPos = new Vector3f(rayStart).add(new Vector3f(direction).mul(t));

        if (t > 0.00001f && t <= maxDistance) {
            return new WorldGuiIntersectionResult(new Vec2(u, v), new Vec3(hitPos.x(), hitPos.y(), hitPos.z()));
        }
        return null;
    }

    public static class WorldGuiIntersectionResult {
        public Vec2 normal;
        public Vec3 pos;

        protected WorldGuiIntersectionResult(Vec2 normal, Vec3 pos) {
            this.normal = normal;
            this.pos = pos;
        }
    }
}
