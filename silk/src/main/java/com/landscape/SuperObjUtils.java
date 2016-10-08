package com.landscape;

import java.lang.reflect.Field;

/**
 * Created by 1 on 2016/10/8.
 */

public class SuperObjUtils {

    public static Object parseParent(Object srcBean) {
        Object destObj = null;
        try {
            if (srcBean.getClass().getName().endsWith("$$Subcriber")) {
                destObj = srcBean.getClass().getSuperclass().newInstance();
            } else {
                destObj = srcBean.getClass().newInstance();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (destObj == null) {
            return destObj;
        }
        Field[] fields = destObj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldObj = field.get(srcBean);
                boolean isPrimitive =
                        (field.getType().isPrimitive() ||
                                (String.class.isAssignableFrom(field.getType())) ||
                                (Number.class.isAssignableFrom(field.getType())) ||
                                (Boolean.class.isAssignableFrom(field.getType())) ||
                                (Character.class.isAssignableFrom(field.getType())));
                if (!isPrimitive) {
                    field.set(destObj, parseParent(fieldObj));
                } else {
                    field.set(destObj, fieldObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return destObj;
    }

    public static Object iteratorClone(Object srcBean) {
        Object destObj = null;
        try {
            destObj = Class.forName(srcBean.getClass().getName() + "$$Subcriber").newInstance();
        } catch (ClassNotFoundException e) {
            try {
                destObj = srcBean.getClass().newInstance();
            } catch (InstantiationException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (destObj == null) {
            return destObj;
        }
        Field[] fields = srcBean.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldObj = field.get(srcBean);
                boolean isPrimitive =
                        (field.getType().isPrimitive() ||
                                (String.class.isAssignableFrom(field.getType())) ||
                                (Number.class.isAssignableFrom(field.getType())) ||
                                (Boolean.class.isAssignableFrom(field.getType())) ||
                                (Character.class.isAssignableFrom(field.getType())));
                if (!isPrimitive) {
                    field.set(destObj, iteratorClone(fieldObj));
                } else {
                    field.set(destObj, fieldObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return destObj;
    }

}
