package com.bignat.viktus.logic;

import com.bignat.viktus.model.Supply;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class StockConsumer {

    // Removes the ingredients from the stock, in place.
    // Stock entries are used in list order and keep their own unit
    // (1 kg minus 300 g becomes 0.7 kg). Empty entries are deleted.
    public static void consume(List<Supply> stock, List<Supply> ingredients) {
        for (Supply ing : ingredients) {
            double need = ing.quantity * ing.unit.factor;
            String wanted = key(ing);

            Iterator<Supply> it = stock.iterator();
            while (it.hasNext() && need > 1e-9) {
                Supply s = it.next();
                if (!key(s).equals(wanted)) continue;

                double base = s.quantity * s.unit.factor;
                double take = Math.min(base, need);
                need -= take;

                double left = base - take;
                if (left <= 1e-9) {
                    it.remove();
                } else {
                    s.quantity = Math.round(left / s.unit.factor * 1000) / 1000.0;
                }
            }
        }
    }

    private static String key(Supply s) {
        return s.unit.dimension.name() + "|" + s.name.trim().toLowerCase(Locale.ROOT);
    }
}
