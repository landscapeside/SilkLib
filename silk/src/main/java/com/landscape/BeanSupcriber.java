package com.landscape;

import rx.subjects.PublishSubject;

/**
 * Created by landscape on 2016/8/16.
 */
public interface BeanSupcriber<T> {
    void sendModeNotify(T bean);
    void notifyNode(String tag, Object bean);
    void setSilkTrigger(PublishSubject silkTrigger);
    void setNodeTrigger(PublishSubject nodeTrigger);
    void setPreTag(String preTag);
    String getPreTag();
}
