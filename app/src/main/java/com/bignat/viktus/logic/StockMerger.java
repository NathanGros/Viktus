package com.bignat.viktus.logic;

import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.List;
import java.util.Locale;

public class StockMerger {

    public static void merge(List<Supply> stock, List<Supply> bought) {
        for (Supply b : bought) {
            boolean unspecified = b.unit == Unit.UNSPECIFIED;
            if (!unspecified && b.quantity <= 1e-9) continue;   // nothing to add

            String name = normalize(b.name);
            Supply target = null;
            boolean nameExists = false;

            for (Supply s : stock) {
                if (!normalize(s.name).equals(name)) continue;
                nameExists = true;
                if (!unspecified && s.unit.dimension == b.unit.dimension) {
                    target = s;
                    break;
                }
            }

            if (unspecified) {
                if (target == null && nameExists) {
                    // Refill an existing unspecified supply that was marked "No"
                    for (Supply s : stock) {
                        if (normalize(s.name).equals(name) && s.unit == Unit.UNSPECIFIED) {
                            s.quantity = 1;
                            break;
                        }
                    }
                } else if (!nameExists) {
                    stock.add(new Supply(b.name, 1, Unit.UNSPECIFIED, false));
                }
            } else if (target != null) {
                // Add in the existing supply's unit (1 kg + 500 g -> 1.5 kg)
                double total = target.quantity * target.unit.factor + b.quantity * b.unit.factor;
                target.quantity = Math.round(total / target.unit.factor * 1000) / 1000.0;
            } else {
                stock.add(new Supply(b.name, b.quantity, b.unit, false));
            }
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}