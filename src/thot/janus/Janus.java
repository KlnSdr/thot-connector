package thot.janus;

import common.logger.Logger;
import dobby.util.json.NewJson;
import thot.janus.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Janus {
    private static final Logger LOGGER = new Logger(Janus.class);

    public static <T extends DataClass> T parse(NewJson json, Class<T> clazz) {
        if (json == null) {
            return null;
        }

        if (!DataClass.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class must implement DataClass");
        }

        final T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            LOGGER.error("Failed to create instance of class: " + clazz.getName());
            LOGGER.trace(e);
            return null;
        }

        final Field[] fields = clazz.getDeclaredFields();
        try {
            for (Field field : fields) {
                if (field.isAnnotationPresent(JanusString.class)) {
                    final JanusString annotation = field.getAnnotation(JanusString.class);
                    final String key = annotation.value();
                    final String value = json.getString(key);
                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, value);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                } else if (field.isAnnotationPresent(JanusInteger.class)) {
                    final JanusInteger annotation = field.getAnnotation(JanusInteger.class);
                    final String key = annotation.value();
                    final Integer value = json.getInt(key);
                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, value);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                } else if (field.isAnnotationPresent(JanusDataClass.class)) {
                    final JanusDataClass annotation = field.getAnnotation(JanusDataClass.class);
                    final String key = annotation.value();
                    final NewJson value = json.getJson(key);

                    final Class<?> dataClass = field.getType();
                    if (!DataClass.class.isAssignableFrom(dataClass)) {
                        throw new IllegalArgumentException("Field must implement DataClass");
                    }

                    @SuppressWarnings("unchecked") final DataClass dataInstance = parse(value,
                            (Class<DataClass>) dataClass);

                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, dataInstance);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                } else if (field.isAnnotationPresent(JanusBoolean.class)) {
                    final JanusBoolean annotation = field.getAnnotation(JanusBoolean.class);
                    final String key = annotation.value();
                    final boolean value = json.getString(key).equalsIgnoreCase("true");
                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, value);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                } else if (field.isAnnotationPresent(JanusUUID.class)) {
                    final JanusUUID annotation = field.getAnnotation(JanusUUID.class);
                    final String key = annotation.value();
                    final UUID value = UUID.fromString(json.getString(key));

                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, value);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                } else if (field.isAnnotationPresent(JanusList.class)) {
                    final JanusList annotation = field.getAnnotation(JanusList.class);
                    final String key = annotation.value();
                    final List<Object> value = json.getList(key);

                    // get the type of the list
                    final Class<?> listType = field.getType();
                    if (!List.class.isAssignableFrom(listType)) {
                        throw new IllegalArgumentException("Field must be a List");
                    }

                    // get the type of the list elements
                    final Class<?> elementType =
                            (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                    // check if the element type is a DataClass
                    final boolean typeIsDataClass = DataClass.class.isAssignableFrom(elementType);

                    // create a new list
                    final List<Object> list = new ArrayList<>();

                    // parse each element in the list
                    for (Object element : value) {
                        if (!typeIsDataClass) {
                            list.add(element);
                            continue;
                        }
                        final NewJson elementJson = (NewJson) element;
                        @SuppressWarnings("unchecked") final DataClass dataInstance = parse(elementJson,
                                (Class<DataClass>) elementType);
                        list.add(dataInstance);
                    }

                    final boolean isPrivate = !field.canAccess(instance);
                    if (isPrivate) {
                        field.setAccessible(true);
                    }
                    field.set(instance, list);
                    if (isPrivate) {
                        field.setAccessible(false);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to set field value");
            LOGGER.trace(e);
            return null;
        }
        return instance;
    }
}
