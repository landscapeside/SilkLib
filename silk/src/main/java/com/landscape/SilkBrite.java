package com.landscape;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.orhanobut.logger.AndroidLogTool;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private static void initLog() {
        Logger.init("Silk V1.0")
                .methodCount(3)
                .methodOffset(2)
                .logTool(new AndroidLogTool());
    }

    @CheckResult
    @NonNull
    public static SilkBrite create() {
        initLog();
        return new SilkBrite(new SilkLog() {
            @Override
            public void log(String message) {
                if (message.startsWith("{") || message.startsWith("[")) {
                    Logger.json(message);
                } else {
                    Logger.d(message);
                }
            }
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
    private final PublishSubject<SilkMsg> nodeTrigger = PublishSubject.create();

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
                .map(new Func1<T, T>() {
                    @Override
                    public T call(T t) {
                        return silkBeanDriver.getSilkBean();
                    }
                })
                .filter(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T t) {
                        if (SilkBrite.this.logger != null) {
                            SilkBrite.this.logger.log(JSONS.parseJson(SuperObjUtils.parseParent(t)));
                        }
                        return true;
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (SilkBrite.this.silkBeanDriver.getSilkBean() == null) {
                            throw new IllegalStateException(
                                    "Cannot subscribe to observable for the bean is null.");
                        }
                        SilkBrite.this.silkBeanDriver.setTrigger(SilkBrite.this.triggers);
                    }
                });
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                queryObservable.unsafeSubscribe(subscriber);
            }
        });
    }

    public <N> Observable<N> asNodeObservable(final String nodeName) {
        final String[] nodes = nodeName.split("::");
        List<String> nodeList = new ArrayList<>(Arrays.asList(nodes));
        Observable<N> nodeObservable = nodeTrigger
                .startWith(new SilkMsg(nodeName,requestField(nodeList,silkBeanDriver.getSilkBean())))
                .subscribeOn(scheduler)
                .onBackpressureLatest()
                .filter(new Func1<SilkMsg, Boolean>() {
                    @Override
                    public Boolean call(SilkMsg silkMsg) {
                        String tag = silkMsg.getTag();
                        if (!TextUtils.isEmpty(tag)) {
                            if (tag.startsWith("::")) {
                                tag = tag.substring(2);
                            }
                            if (tag.endsWith("::")) {
                                tag = tag.substring(0, tag.length() - 2);
                            }
                            if (tag.equals(nodeName)) {
                                return true;
                            }
                        }
                        return false;
                    }
                })
                .map(new Func1<SilkMsg, Object>() {
                    @Override
                    public Object call(SilkMsg t) {
                        return t.getSilkObj();
                    }
                })
                .map(new Func1<Object, N>() {
                    @Override
                    public N call(Object obj) {
                        return (N) obj;
                    }
                })
                .filter(new Func1<N, Boolean>() {
                    @Override
                    public Boolean call(N t) {
                        if (SilkBrite.this.logger != null) {
                            SilkBrite.this.logger.log(JSONS.parseJson(t));
                        }
                        return true;
                    }
                })
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (SilkBrite.this.silkBeanDriver.getSilkBean() == null) {
                            throw new IllegalStateException(
                                    "Cannot subscribe to observable for the bean is null.");
                        }
                        SilkBrite.this.silkBeanDriver.setNodeTrigger(nodeName, SilkBrite.this.nodeTrigger);
                    }
                });
        return nodeObservable;
    }

    private Object requestField(List<String> nodes, Object object) {
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
                    }
                } else {
                    try {
                        nodes.remove(0);
                        field.setAccessible(true);
                        return requestField(nodes, field.get(object));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return object;
    }

}
