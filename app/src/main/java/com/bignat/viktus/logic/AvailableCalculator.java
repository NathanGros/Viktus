package com.bignat.viktus.logic;

import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvailableCalculator {

    // Stock minus what the planned meals need. Fully used supplies are left out.
    public static List<Supply> compute(List<Supply> stock, List<Meal> meals) {
        // 1. Total still to be covered per ingredient (in base units)
        Map<String, Double> remainingNeed = new HashMap<>();
        for (Meal meal : meals) {
            for (Supply ing : meal.supplies) {
                if (ing.unit == Unit.UNSPECIFIED) continue;
                String k = key(ing);
                Double current = remainingNeed.get(k);
                remainingNeed.put(k, (current == null ? 0 : current) + ing.quantity * ing.unit.factor);
            }
        }

        // 2. Take it out of the stock entries, in list order.
        //    Each entry keeps its own unit (e.g. 1 kg minus 300 g shows 0.7 kg).
        List<Supply> result = new ArrayList<>();
        for (Supply s : stock) {
            String k = key(s);
            double base = s.quantity * s.unit.factor;
            double used = 0;

            Double need = remainingNeed.get(k);
            if (need != null && need > 0) {
                used = Math.min(base, need);
                remainingNeed.put(k, need - used);
            }

            double left = base - used;
            if (left > 1e-9) {
                double quantity = Math.round(left / s.unit.factor * 1000) / 1000.0;
                result.add(new Supply(s.name, quantity, s.unit));
            }
        }
        return result;
    }

    private static String key(Supply s) {
        return s.unit.dimension.name() + "|" + s.name.trim().toLowerCase(Locale.ROOT);
    }
}
