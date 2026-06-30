package com.cmdpro.databank.specialconditions;

import net.minecraft.resources.Identifier;

import java.util.HashMap;

public class DatabankSpecialConditionManager {
    protected static HashMap<Identifier, DatabankSpecialCondition> events = new HashMap<>();
    public static void init() {
        for (DatabankSpecialCondition i : events.values()) {
            i.isActive = i.checkActive();
        }
    }
    public static void addEvent(Identifier id, DatabankSpecialCondition event) {
        events.put(id, event);
        event.id = id;
    }
    public static void removeEvent(Identifier id) {
        events.remove(id);
    }
    public static DatabankSpecialCondition getEvent(Identifier id) {
        return events.get(id);
    }
}
