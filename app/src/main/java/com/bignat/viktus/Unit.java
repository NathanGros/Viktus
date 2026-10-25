package com.bignat.viktus;

public enum Unit {
    PIECE("pcs", Dimension.COUNT, 1),
    GRAM("g", Dimension.MASS, 1),
    KILOGRAM("kg", Dimension.MASS, 1000),
    MILLILITER("ml", Dimension.VOLUME, 1),
    LITER("L", Dimension.VOLUME, 1000),
    TABLESPOON("tbsp", Dimension.VOLUME, 15),
    TEASPOON("tsp", Dimension.VOLUME, 5),
    CUP("cup", Dimension.VOLUME, 240);

    public enum Dimension { COUNT, MASS, VOLUME }

    public final String label;
    public final Dimension dimension;
    public final double factor;   // how many base units (pcs / g / ml) in one of this unit

    Unit(String label, Dimension dimension, double factor) {
        this.label = label;
        this.dimension = dimension;
        this.factor = factor;
    }
}