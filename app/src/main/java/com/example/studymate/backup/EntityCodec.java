package com.example.studymate.backup;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reflection-based converter between entity objects (public fields) and plain maps. */
public final class EntityCodec {
    private EntityCodec() {}

    public static Map<String, Object> toMap(Object entity) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Field f : entity.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                m.put(f.getName(), f.get(entity));
            } catch (IllegalAccessException ignored) { }
        }
        return m;
    }

    public static <T> T fromMap(Class<T> cls, Map<String, Object> m) {
        try {
            T o = cls.getDeclaredConstructor().newInstance();
            for (Field f : cls.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !m.containsKey(f.getName())) continue;
                Object v = m.get(f.getName());
                String s = v == null ? null : String.valueOf(v);
                Class<?> t = f.getType();
                if (t == String.class) {
                    f.set(o, s);
                } else if (s == null || s.trim().isEmpty()) {
                    continue;
                } else if (t == long.class) {
                    f.setLong(o, (long) Double.parseDouble(s));
                } else if (t == int.class) {
                    f.setInt(o, (int) Double.parseDouble(s));
                } else if (t == double.class) {
                    f.setDouble(o, Double.parseDouble(s));
                } else if (t == boolean.class) {
                    f.setBoolean(o, Boolean.parseBoolean(s) || s.equals("1"));
                }
            }
            return o;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build " + cls.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
