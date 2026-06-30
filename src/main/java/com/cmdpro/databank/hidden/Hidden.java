package com.cmdpro.databank.hidden;

import net.minecraft.resources.Identifier;

public class Hidden {
    public Hidden(HiddenTypeInstance<?> type, HiddenCondition condition) {
        this.type = type;
        this.condition = condition;
        this.type.hidden = this;
    }
    public Identifier id;
    public HiddenTypeInstance<?> type;
    public HiddenCondition condition;
}
