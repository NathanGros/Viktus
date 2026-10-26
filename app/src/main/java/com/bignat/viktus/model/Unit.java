package com.bignat.viktus.model;

import androidx.annotation.StringRes;
import com.bignat.viktus.R;

public enum Unit {
    PIECE(R.string.unit_piece, Dimension.COUNT, 1),
    GRAM(R.string.unit_gram, Dimension.MASS, 1),
    KILOGRAM(R.string.unit_kilogram, Dimension.MASS, 1000),
    MILLILITER(R.string.unit_milliliter, Dimension.VOLUME, 1),
    LITER(R.string.unit_liter, Dimension.VOLUME, 1000),
    TABLESPOON(R.string.unit_tablespoon, Dimension.VOLUME, 15),
    TEASPOON(R.string.unit_teaspoon, Dimension.VOLUME, 5),
    CUP(R.string.unit_cup, Dimension.VOLUME, 240);

    public enum Dimension { COUNT, MASS, VOLUME }

    public final int labelRes;
    public final Dimension dimension;
    public final double factor;   // how many base units (pcs / g / ml) in one of this unit

    Unit(@StringRes int labelRes, Dimension dimension, double factor) {
        this.labelRes = labelRes;
        this.dimension = dimension;
        this.factor = factor;
    }
}