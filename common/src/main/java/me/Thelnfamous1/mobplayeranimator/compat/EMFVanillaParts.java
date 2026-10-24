package me.Thelnfamous1.mobplayeranimator.compat;

import net.minecraft.client.model.geom.ModelPart;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

final class EMFVanillaParts {
    private static final Map<ModelPart, Map<String, ModelPart>> CACHE = new WeakHashMap<>();
    private static final Map<Class<?>, Method> METHODS = new HashMap<>();
    private static final Map<Class<?>, Field> NAME_FIELDS = new HashMap<>();

    private EMFVanillaParts() {
    }

    static void clearCache() {
        CACHE.clear();
    }

    static Map<String, ModelPart> byName(ModelPart root) {
        if (root == null) {
            return Collections.emptyMap();
        }
        Map<String, ModelPart> cached = CACHE.get(root);
        if (cached != null) {
            return cached;
        }
        Map<String, ModelPart> result = read(root);
        CACHE.put(root, result);
        return result;
    }

    private static Map<String, ModelPart> read(ModelPart root) {
        Method method = methodFor(root.getClass());
        if (method == null) {
            return Collections.emptyMap();
        }
        try {
            if (!(method.invoke(root) instanceof Iterable<?> parts)) {
                return Collections.emptyMap();
            }
            Map<String, ModelPart> result = new HashMap<>();
            for (Object candidate : parts) {
                if (candidate instanceof ModelPart part) {
                    String name = nameOf(candidate);
                    if (name != null) {
                        result.put(name, part);
                    }
                }
            }
            return result.isEmpty() ? Collections.emptyMap() : result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    private static Method methodFor(Class<?> type) {
        if (METHODS.containsKey(type)) {
            return METHODS.get(type);
        }
        Method method = null;
        try {
            method = type.getMethod("getAllVanillaPartsEMF");
            method.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            method = null;
        }
        METHODS.put(type, method);
        return method;
    }

    private static String nameOf(Object part) {
        Class<?> type = part.getClass();
        if (!NAME_FIELDS.containsKey(type)) {
            Field found = null;
            for (Class<?> cursor = type; cursor != null && found == null; cursor = cursor.getSuperclass()) {
                try {
                    found = cursor.getDeclaredField("name");
                    found.setAccessible(true);
                } catch (NoSuchFieldException ignored) {
                    found = null;
                } catch (RuntimeException e) {
                    break;
                }
            }
            NAME_FIELDS.put(type, found);
        }
        Field field = NAME_FIELDS.get(type);
        if (field == null) {
            return null;
        }
        try {
            return field.get(part) instanceof String name ? name : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
