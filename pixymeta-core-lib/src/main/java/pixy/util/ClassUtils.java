package pixy.util;

/**
 * Created by EVE on 01.08.2016.
 */
public class ClassUtils {
    /** anonymous class return "" as simple name */
    public static String getSimpleClassName(Object item) {
        Class<?> itemClass = item.getClass();
        while (itemClass.isAnonymousClass()) {
            itemClass = itemClass.getSuperclass();
        }
        return itemClass.getSimpleName();
    }
}
