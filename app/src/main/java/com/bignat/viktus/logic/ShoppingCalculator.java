package com.bignat.viktus.logic;

import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        // 1. Needs per ingredient; unspecified ones are tracked separately by name
        Map<String, Need> needs = new LinkedHashMap<>();
        Map<String, String> unspecifiedNeeds = new LinkedHashMap<>();   // lowercase -> display name
        for (Meal meal : meals) {
            for (Supply ing : meal.supplies) {
                if (ing.unit == Unit.UNSPECIFIED) {
                    String n = ing.name.trim().toLowerCase(Locale.ROOT);
                    if (!unspecifiedNeeds.containsKey(n)) unspecifiedNeeds.put(n, ing.name.trim());
                    continue;
                }
                String key = key(ing);
                Need need = needs.get(key);
                if (need == null) {
                    need = new Need(ing.name.trim(), ing.unit.dimension);
                    needs.put(key, need);
                }
                need.base += ing.quantity * ing.unit.factor;
            }
        }

        // 2. Stock: totals per ingredient, plus which names we own at all
        Map<String, Double> stocked = new HashMap<>();
        Set<String> ownedNames = new HashSet<>();
        Set<String> unspecifiedStock = new HashSet<>();
        for (Supply s : stock) {
            if (s.quantity <= 1e-9) continue;   // empty supplies (any unit) don't count as owned
            String n = s.name.trim().toLowerCase(Locale.ROOT);
            ownedNames.add(n);
            if (s.unit == Unit.UNSPECIFIED) {
                unspecifiedStock.add(n);
                continue;
            }
            String key = key(s);
            Double current = stocked.get(key);
            stocked.put(key, (current == null ? 0 : current) + s.quantity * s.unit.factor);
        }

        // 3. What is missing
        List<Supply> result = new ArrayList<>();
        Set<String> listed = new HashSet<>();
        for (Map.Entry<String, Need> e : needs.entrySet()) {
            String n = e.getValue().name.toLowerCase(Locale.ROOT);
            if (unspecifiedStock.contains(n)) continue;   // we own some, amount unknown

            Double have = stocked.get(e.getKey());
            double missing = e.getValue().base - (have == null ? 0 : have);
            if (missing > 1e-9) {
                result.add(toSupply(e.getValue(), missing));
                listed.add(n);
            }
        }

        // Unspecified needs: listed only if we own none of it
        for (Map.Entry<String, String> e : unspecifiedNeeds.entrySet()) {
            if (!ownedNames.contains(e.getKey()) && !listed.contains(e.getKey())) {
                result.add(new Supply(e.getValue(), 1, Unit.UNSPECIFIED));
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