package com.cmdpro.databank.advancement;

import com.cmdpro.databank.hidden.Hidden;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public interface ClientAdvancementListener {
    List<ClientAdvancementListener> ADVANCEMENT_LISTENERS = new ArrayList<>();
    default void onLock(Identifier locked) {}
    default void onUnlock(Identifier unlocked) {}
    default void onLock(List<Identifier> locked) {}
    default void onUnlock(List<Identifier> unlocked) {}
}
