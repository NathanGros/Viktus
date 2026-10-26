package com.bignat.viktus.logic;

import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShoppingCalculator {

    private static class Need {
        final String name;                 // name as first written in a meal
        final Unit.Dimension dimension;
        double base;                       // total in base units (pcs / g / ml)

        Need(String name, Unit.Dimension dimension) {
            this.name = name;
            this.dimension = dimension;
        }
    }

    // Everything the meals need, minus what the stock holds
    public static List<Supply> compute(List<Meal> meals, List<Supply> stock) {
        // 1. Total needed per ingredient, summed across all meals
        Map<String, Need> needs = new LinkedHashMap<>();
        for (Meal meal : meals) {
            for (Supply ing : meal.supplies) {
                String key = key(ing);
                Need need = needs.get(key);
                if (need == null) {
                    need = new Need(ing.name.trim(), ing.unit.dimension);
                    needs.put(key, need);
                }
                need.base += ing.quantity * ing.unit.factor;
            }
        }

        // 2. Total in stock per ingredient
        Map<String, Double> stocked = new HashMap<>();
        for (Supply s : stock) {
            String key = key(s);
            Double current = stocked.get(key);
            stocked.put(key, (current == null ? 0 : current) + s.quantity * s.unit.factor);
        }

        // 3. Keep only what is missing
        List<Supply> result = new ArrayList<>();
        for (Map.Entry<String, Need> e : needs.entrySet()) {
            Double have = stocked.get(e.getKey());
            double missing = e.getValue().base - (have == null ? 0 : have);
            if (missing > 1e-9) {
                result.add(toSupply(e.getValue(), missing));
            }
        }
        return result;
    }

    // Same matching rule as StockChecker: name ignoring case + same unit family
    private static String key(Supply s) {
        return s.unit.dimension.name() + "|" + s.name.trim().toLowerCase(Locale.ROOT);
    }

    // Turn an amount in base units into a readable quantity (1500 g -> 1.5 kg)
    private static Supply toSupply(Need need, double missing) {
        switch (need.dimension) {
            case MASS:
                return missing >= 1000
                        ? new Supply(need.name, round(missing / 1000), Unit.KILOGRAM)
                        : new Supply(need.name, round(missing), Unit.GRAM);
            case VOLUME:
                return missing >= 1000
                        ? new Supply(need.name, round(missing / 1000), Unit.LITER)
                        : new Supply(need.name, round(missing), Unit.MILLILITER);
            default:   // COUNT: you can't buy 0.3 of an egg, so round up
                return new Supply(need.name, Math.ceil(missing - 1e-9), Unit.PIECE);
        }
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}