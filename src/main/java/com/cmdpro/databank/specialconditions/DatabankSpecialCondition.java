package com.cmdpro.databank.specialconditions;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.Map;

public abstract class DatabankSpecialCondition {
    public Identifier id;
    public boolean isActive;
    public abstract boolean checkActive();
    public boolean isActiveParams(Map<String, Object> params) {
        return isActive;
    }
}
