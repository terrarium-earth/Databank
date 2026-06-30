package com.cmdpro.databank.worldgui.components.types;

public abstract class WorldGuiRectComponent extends WorldGuiPositionedComponent {
    public int width;
    public int height;

    public WorldGuiRectComponent(int x, int y, int width, int height) {
        super(x, y);
        this.width = width;
        this.height = height;
    }
}
