package com.landscape;

/**
 * Created by 1 on 2016/10/12.
 */

public class SilkMsg {
    public SilkMsg() {

    }

    public SilkMsg(String tag, Object silkObj) {
        this.tag = tag;
        this.silkObj = silkObj;
    }

    public SilkMsg(Object silkObj) {
        this.silkObj = silkObj;
    }

    private String tag;
    private Object silkObj;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getSilkObj() {
        return silkObj;
    }

    public void setSilkObj(Object silkObj) {
        this.silkObj = silkObj;
    }
}
