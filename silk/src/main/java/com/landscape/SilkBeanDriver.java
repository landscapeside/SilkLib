package com.landscape;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.subjects.PublishSubject;

/**
 * Created by 1 on 2016/9/1.
 */
public class SilkBeanDriver<T> {

    T silkBean = null;

    public SilkBeanDriver<T> asSilkBean(T srcBean) {
        // TODO: 2016/9/1 遍历
        silkBean = (T) iteratorClone(srcBean);
        return this;
    }

    private Object iteratorClone(Object srcBean) {
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

    boolean hasChanged = false;
    public SilkBeanDriver<T> updateBean(T srcBean) {
        hasChanged = false;
        iteratorCopy(srcBean,silkBean);
        if (hasChanged) {
            Method sendMethod = null;
            try {
                sendMethod = silkBean.getClass().getMethod("sendTrigger",new Class[]{srcBean.getClass()});
                sendMethod.invoke(silkBean, silkBean);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    private void iteratorCopy(Object srcObj,Object destObj) {
        Field[] fields = srcObj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldObj = field.get(srcObj);
                boolean isPrimitive =
                        (field.getType().isPrimitive() ||
                                (String.class.isAssignableFrom(field.getType())) ||
                                (Number.class.isAssignableFrom(field.getType())) ||
                                (Boolean.class.isAssignableFrom(field.getType())) ||
                                (Character.class.isAssignableFrom(field.getType())));
                if (!isPrimitive) {
                    iteratorCopy(fieldObj,field.get(destObj));
                } else {
                    Object destVal = field.get(destObj);
                    if (fieldObj.hashCode() != destVal.hashCode() && !fieldObj.equals(destVal)) {
                        hasChanged = true;
                        field.set(destObj,fieldObj);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setTrigger(PublishSubject triggers) {
        ((BeanSupcriber) silkBean).setSilkTrigger(triggers);
        List<BeanSupcriber> subSilkBeans = iteratorBean(silkBean);
        for (BeanSupcriber beanSupcriber : subSilkBeans) {
            beanSupcriber.setSilkTrigger(triggers);
        }
    }

    public List<BeanSupcriber> iteratorBean(Object srcObj) {
        if (srcObj == null) {
            return new ArrayList<>();
        }
        List<BeanSupcriber> subSilkBeans = new ArrayList<>();
        Field[] fields;
        if (srcObj.getClass().getName().contains("$$Subcriber")) {
            fields = srcObj.getClass().getSuperclass().getDeclaredFields();
        } else {
            fields = srcObj.getClass().getDeclaredFields();
        }
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldObj = field.get(srcObj);
                boolean isPrimitive =
                        (field.getType().isPrimitive() ||
                                (String.class.isAssignableFrom(field.getType())) ||
                                (Number.class.isAssignableFrom(field.getType())) ||
                                (Boolean.class.isAssignableFrom(field.getType())) ||
                                (Character.class.isAssignableFrom(field.getType())));
                if (!isPrimitive) {
                    if (fieldObj.getClass().getName().contains("$$Subcriber")) {
                        subSilkBeans.add((BeanSupcriber) fieldObj);
                    }
                    if (!field.getName().equals("silkTrigger")) {
                        subSilkBeans.addAll(iteratorBean(field.get(srcObj)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return subSilkBeans;
    }

    public T getSilkBean() {
        return silkBean;
    }
}
