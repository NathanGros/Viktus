package com.bignat.viktus.logic;

import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.List;
import java.util.Locale;

public class StockChecker {

    // True if the stock holds at least the ingredient's quantity.
    // Same name (ignoring case), compatible unit family, quantities summed after conversion.
    public static boolean hasEnough(Supply ingredient, List<Supply> stock) {
        String wanted = normalize(ingredient.name);
        double available = 0;

        for (Supply s : stock) {
            if (!normalize(s.name).equals(wanted)) continue;
            if (s.quantity <= 1e-9) continue;   // empty stock doesn't count

            // "Unspecified" on either side: owning the item is enough
            if (ingredient.unit == Unit.UNSPECIFIED || s.unit == Unit.UNSPECIFIED) return true;

            if (s.unit.dimension == ingredient.unit.dimension) {
                available += s.quantity * s.unit.factor;
            }
        }
        double needed = ingredient.quantity * ingredient.unit.factor;
        return available + 1e-9 >= needed;
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}