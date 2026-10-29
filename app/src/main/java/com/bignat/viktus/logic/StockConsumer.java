package com.bignat.viktus.logic;

import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class StockConsumer {

    // Removes the ingredients from the stock, in place.
    // Stock entries are used in list order and keep their own unit
    // (1 kg minus 300 g becomes 0.7 kg)..
    public static void consume(List<Supply> stock, List<Supply> ingredients) {
        for (Supply ing : ingredients) {
            if (ing.unit == Unit.UNSPECIFIED) continue;
            double need = ing.quantity * ing.unit.factor;
            String wanted = key(ing);

            for (Supply s : stock) {
                if (need <= 1e-9) break;
                if (!key(s).equals(wanted)) continue;

                double base = s.quantity * s.unit.factor;
                double take = Math.min(base, need);
                need -= take;

                double left = base - take;
                s.quantity = left <= 1e-9 ? 0 : Math.round(left / s.unit.factor * 1000) / 1000.0;
            }
        }
    }

    private static String key(Supply s) {
        return s.unit.dimension.name() + "|" + s.name.trim().toLowerCase(Locale.ROOT);
    }
}
