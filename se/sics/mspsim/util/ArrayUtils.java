package se.sics.mspsim.util;

public class ArrayUtils {

  public static Object[] add(Class<?> componentType, Object[] array, Object value) {
    Object[] tmp;
    if (array == null) {
      tmp = (Object[]) java.lang.reflect.Array.newInstance(componentType, 1);
    } else {
      tmp = (Object[]) java.lang.reflect.Array.newInstance(componentType, array.length + 1);
      System.arraycopy(array, 0, tmp, 0, array.length);
    }
    tmp[tmp.length - 1] = value;
    return tmp;
  }

  public static Object[] remove(Object[] array, Object value) {
    if (array != null) {
      for (int index = 0, n = array.length; index < n; index++) {
        if (value.equals(array[index])) {
          if (n == 1) {
            return null;
          }
          Object[] tmp = (Object[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length - 1);
          if (index > 0) {
            System.arraycopy(array, 0, tmp, 0, index);
          }
          if (index < tmp.length) {
            System.arraycopy(array, index + 1, tmp, index, tmp.length - index);
          }
          return tmp;
        }
      }
    }
    return array;
  }

}
