package com.landscape;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.subjects.PublishSubject;

/**
 * Created by 1 on 2016/9/1.
 */
public class SilkBeanDriver<T> {

    T silkBean = null;

    public SilkBeanDriver<T> asSilkBean(T srcBean) {
        // TODO: 2016/9/1 遍历
        silkBean = (T) SuperObjUtils.iteratorClone(srcBean);
        return this;
    }

    boolean hasChanged = false;
    public SilkBeanDriver<T> updateBean(T srcBean) {
        hasChanged = false;
        iteratorCopy(srcBean,silkBean);
        if (hasChanged) {
            ((BeanSupcriber) silkBean).sendModeNotify(silkBean);
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
                        if (destObj.getClass().getName().contains("$$Subcriber")) {
                            ((BeanSupcriber)destObj).notifyNode(field.getName(),fieldObj);
                        }
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

    public void setNodeTrigger(String tag,PublishSubject triggers) {
        ((BeanSupcriber) silkBean).setNodeTrigger(triggers);
        List<BeanSupcriber> subSilkBeans = iteratorBean(silkBean);
        for (BeanSupcriber beanSupcriber : subSilkBeans) {
            beanSupcriber.setNodeTrigger(triggers);
        }
        if (!TextUtils.isEmpty(tag)) {
            String[] nodes = tag.split("::");
            List<String> nodeList = new ArrayList<>(Arrays.asList(nodes));
            iteratorSetTag("",nodeList,silkBean);
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
                    if (!field.getName().equals("silkTrigger") || !field.getName().equals("nodeTrigger")) {
                        subSilkBeans.addAll(iteratorBean(field.get(srcObj)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return subSilkBeans;
    }

    private void iteratorSetTag(String preTag,List<String> nodes,Object object) {
        if (nodes.size() == 0) {
            return;
        }
        Field[] fields;
        if (object.getClass().getName().contains("$$Subcriber")) {
            fields = object.getClass().getSuperclass().getDeclaredFields();
        } else {
            fields = object.getClass().getDeclaredFields();
        }
        for (Field field : fields) {
            if (field.getName().equals(nodes.get(0))) {
                if (object.getClass().getName().contains("$$Subcriber")) {
                    ((BeanSupcriber)object).setPreTag(preTag);
                }
                if (nodes.size() > 1) {
                    if (TextUtils.isEmpty(preTag)) {
                        preTag = nodes.get(0);
                    } else {
                        preTag = preTag + "::" + nodes.get(0);
                    }
                    nodes.remove(0);
                    try {
                        field.setAccessible(true);
                        iteratorSetTag(preTag, nodes, field.get(object));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    return;
                }
            }
        }
    }

    public T getSilkBean() {
        return silkBean;
    }
}
