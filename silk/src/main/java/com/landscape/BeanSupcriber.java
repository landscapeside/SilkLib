package com.landscape;

import android.content.ContentValues;

import java.util.Map;

import rx.subjects.PublishSubject;

/**
 * Created by landscape on 2016/8/16.
 */
public interface BeanSupcriber<T> {
    void sendTrigger(T bean);
    void setSilkTrigger(PublishSubject silkTrigger);
}
