package com.example.labelmaker;

/**
 * Represents a single column in the dynamic rate list spreadsheet.
 * Stores the column's display name and its proportional layout weight.
 */
public class ColumnConfig {

    private String name;
    private int width;
    private float customFontSize = -1f;

    public ColumnConfig(String name, int width) {
        this.name = name;
        this.width = width;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public float getCustomFontSize() {
        return customFontSize;
    }

    public void setCustomFontSize(float customFontSize) {
        this.customFontSize = customFontSize;
    }
}
