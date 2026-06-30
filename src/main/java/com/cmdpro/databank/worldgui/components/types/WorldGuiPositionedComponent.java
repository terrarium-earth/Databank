package com.cmdpro.databank.worldgui.components.types;

import com.cmdpro.databank.worldgui.components.WorldGuiComponent;

public abstract class WorldGuiPositionedComponent extends WorldGuiComponent {
    public int x;
    public int y;

    public WorldGuiPositionedComponent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
