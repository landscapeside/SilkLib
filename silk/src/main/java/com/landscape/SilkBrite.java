package com.landscape;

import android.content.ContentValues;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by 1 on 2016/8/15.
 */
public class SilkBrite<T> {

    SilkBeanDriver<T> silkBeanDriver = new SilkBeanDriver<>();

    private final SilkLog logger;

    @CheckResult
    @NonNull
    public static SilkBrite create() {
        return new SilkBrite(message -> {
        });
    }

    @CheckResult
    @NonNull
    public static SilkBrite create(SilkLog log) {
        return new SilkBrite(log);
    }

    private SilkBrite(SilkLog log) {
        logger = log;
    }

    private final PublishSubject<T> triggers = PublishSubject.create();
    private final PublishSubject<Map> nodeTrigger = PublishSubject.create();

    private final Scheduler scheduler = Schedulers.io();

    public T asSilkBean() {
        return silkBeanDriver.getSilkBean();
    }

    public T asSilkBean(T srcBean) {
        return silkBeanDriver.asSilkBean(srcBean).getSilkBean();
    }

    public void updateBean(T srcBean) {
        if (silkBeanDriver.getSilkBean().getClass().isAssignableFrom(srcBean.getClass())) {
            throw new IllegalArgumentException("必须传入相同类型");
        }
        silkBeanDriver.updateBean(srcBean);
    }

    public Observable<T> asModeObservable() {
        final Observable<T> queryObservable = triggers
                .startWith(silkBeanDriver.getSilkBean())
                .subscribeOn(scheduler)
                .onBackpressureLatest() // Guard against uncontrollable frequency of scheduler executions.
                .map(t -> silkBeanDriver.getSilkBean())
                .doOnSubscribe(() -> {
                    if (silkBeanDriver.getSilkBean() == null) {
                        throw new IllegalStateException(
                                "Cannot subscribe to observable for the bean is null.");
                    }
                    silkBeanDriver.setTrigger(triggers);
                });
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                queryObservable.unsafeSubscribe(subscriber);
            }
        });
    }

    public <N> Observable<N> asNodeObservable(String nodeName) {
        final String[] nodes = nodeName.split("::");
        Observable<N> nodeObservable = triggers
                .startWith(silkBeanDriver.getSilkBean())
                .subscribeOn(scheduler)
                .onBackpressureLatest()
                .map(t -> silkBeanDriver.getSilkBean())
                .map(t -> {
                    Map values = null;
                    List<String> nodeList = new ArrayList<>(Arrays.asList(nodes));
                    try {
                        Object object = requestField(nodeList,t);
                        values = new HashMap();
                        values.put(nodeName, object);
                    } catch (FieldNotMatchedException e) {
                        values = null;
                    }
                    return values;
                })
                .filter(map -> {
                    if (map == null) {
                        return false;
                    }
                    return true;
                })
                .map(map -> (N) map.get(nodeName))
                .doOnSubscribe(() -> {
                    if (silkBeanDriver.getSilkBean() == null) {
                        throw new IllegalStateException(
                                "Cannot subscribe to observable for the bean is null.");
                    }
                    silkBeanDriver.setTrigger(triggers);
                });
        return nodeObservable;
    }

    private Object requestField(List<String> nodes, Object object) throws FieldNotMatchedException {
        Field[] fields;
        if (object.getClass().getName().contains("$$Subcriber")) {
            fields = object.getClass().getSuperclass().getDeclaredFields();
        } else {
            fields = object.getClass().getDeclaredFields();
        }
        for (Field field : fields) {
            if (field.getName().equals(nodes.get(0))) {
                if (nodes.size() == 1) {
                    try {
                        field.setAccessible(true);
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new FieldNotMatchedException();
                    }
                } else {
                    try {
                        nodes.remove(0);
                        field.setAccessible(true);
                        return requestField(nodes, field.get(object));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new FieldNotMatchedException();
                    }
                }
            }
        }
        throw new FieldNotMatchedException();
    }

}
