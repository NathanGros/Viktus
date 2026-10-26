package com.bignat.viktus.data;

import androidx.room.TypeConverter;

import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Converters {

    @TypeConverter
    public static String fromUnit(Unit unit) {
        return unit == null ? null : unit.name();
    }

    @TypeConverter
    public static Unit toUnit(String name) {
        return name == null ? null : Unit.valueOf(name);
    }

    @TypeConverter
    public static String fromIngredients(List<Supply> list) {
        JSONArray array = new JSONArray();
        if (list != null) {
            for (Supply s : list) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", s.name);
                    o.put("quantity", s.quantity);
                    o.put("unit", s.unit.name());
                    array.put(o);
                } catch (JSONException ignored) { }
            }
        }
        return array.toString();
    }

    @TypeConverter
    public static List<Supply> toIngredients(String json) {
        List<Supply> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                list.add(new Supply(o.getString("name"), o.getDouble("quantity"),
                        Unit.valueOf(o.getString("unit"))));
            }
        } catch (JSONException ignored) { }
        return list;
    }
}