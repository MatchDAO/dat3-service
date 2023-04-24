package com.chat.entity;


public class ValidationFailed {

    private long times;
    private long time;

    public ValidationFailed(long times, long time) {
        this.times = times;
        this.time = time;
    }

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }


}
