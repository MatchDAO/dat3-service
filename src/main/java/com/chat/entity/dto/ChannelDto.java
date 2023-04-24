package com.chat.entity.dto;

public class ChannelDto {
    //发起人
    private String from;

    //接收人
    private String to;

    //通道
    private String channel;

    //开始时间
    private Long begin;

    //结束时间
    private Long end;

    //当前时间 最后一次扣费时间
    private Long current;

    //扣费金额 2222;3333;
    private String frozen;

    //扣费次数,实际消费时长 unit:min
    private Integer frozenTimes;

    //结束原因 1:from挂断 2:to挂断 3:from资金不足 4未知原因(from/to未知原因中断),5to拒绝
    private Integer reason;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getBegin() {
        return begin;
    }

    public void setBegin(Long begin) {
        this.begin = begin;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public String getFrozen() {
        return frozen;
    }

    public void setFrozen(String frozen) {
        this.frozen = frozen;
    }

    public Integer getFrozenTimes() {
        return frozenTimes;
    }

    public void setFrozenTimes(Integer frozenTimes) {
        this.frozenTimes = frozenTimes;
    }

    public Integer getReason() {
        return reason;
    }

    public void setReason(Integer reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "ChannelDto{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", channel='" + channel + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                ", current=" + current +
                ", frozen='" + frozen + '\'' +
                ", frozenTimes=" + frozenTimes +
                ", reason=" + reason +
                '}';
    }
}
