package com.chat.task.queue;

import cn.hutool.json.JSONArray;
import com.chat.cache.RtcSessionCache;
import com.chat.cache.UserCache;
import com.chat.common.InteractiveStautsEnum;
import com.chat.common.R;
import com.chat.common.TokenEnum;
import com.chat.config.ChatConfig;
import com.chat.entity.Interactive;
import com.chat.entity.User;
import com.chat.entity.dto.ChannelDto;
import com.chat.entity.dto.PriceGradeUserDto;
import com.chat.entity.dto.WalletUserResult;
import com.chat.service.AgoraService;
import com.chat.service.AptosService;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.PriceGradeUserServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.Coder;
import com.chat.utils.DESCoder;
import com.chat.utils.MessageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CallDelayTaskRunner implements InitializingBean {
    @Resource
    private DelayQueue<CallDelayTask> myCallDelayQueue;
    @Resource
    private RtcSessionCache rtcSessionCache;
    @Resource
    private UserServiceImpl userService;
    @Resource
    private PriceGradeUserServiceImpl priceGradeUserService;
    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private InteractiveServiceImpl interactiveService;
    @Resource
    private AgoraService agoraService;
    @Resource
    private AptosService aptosService;
    @Resource
    private UserCache userCache;

    //结束原因 1:from挂断 2:to挂断 3:from资金不足 4未知原因(from/to未知原因中断),5to拒绝
    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                log.info("CallDelayTaskRunner {}", myCallDelayQueue.size());
                while (true) {
                    CallDelayTask take = myCallDelayQueue.take();
                    log.info("take:{}", take);
                    log.info("take: {},{},{},{}", take.getChannelName(), take.getDelay(TimeUnit.SECONDS), take.getFrom(), take.getTo());
                    try {
                        long currentTimeMillis = System.currentTimeMillis() / 1000;
                        //获取用户信息
                        String from = take.getFrom();
                        String to = take.getTo();
                        User fromUser = userCache.getUser(from);
                        User toUser = userCache.getUser(to);
                        PriceGradeUserDto grade = priceGradeUserService.grade(to);
                        if (fromUser == null || toUser == null) {
                            log.error("AdvanceDelayTaskRunner :no user info");
                            continue;
                        }
                        ChannelDto channel = rtcSessionCache.channel(from, to);
                        // (room.addr, room.receiver, room.started_at, room.finished_at, room.minute_rate, room.minute, room.deposit, room.done)
                        JSONArray userCall = aptosService.getUserCall(fromUser.getAddress());

                        int size = userCall.size();
                        //每一次扣费
                        if(take.getFlag()==100){
                            if (userCall==null||size==0||(System.currentTimeMillis() / 1000 - (userCall.getJSONObject(size - 1).getLong("time")) > 30))//限定时间内未扣费
                            {
                                myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), 500, 3, take.getBegin()));

                            }
                            continue;
                        }
                        //第一次是否扣费
                        if(take.getFlag()==99){
                            if (userCall==null||size==0||(System.currentTimeMillis() / 1000 - (userCall.getJSONObject(size - 1).getLong("time")) > 30))//限定时间内未扣费
                            {
                                myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), 500, 3, take.getBegin()));

                            }
                            continue;
                        }
                        //余额 第59秒计算
                        if(take.getFlag()==59){
                            JSONArray userAssets = aptosService.getUserAssets(fromUser.getAddress());
                            if(userAssets!=null&&userAssets.getLong(5)>=new BigInteger(grade.getEPrice()).longValue() ){
                                long d = currentTimeMillis  - take.getBegin()/1000;
                                long mo = d  % 60;
                                Long current = 60L ;
                                //时间节点每分钟过了几秒 时间归正为每分钟第54
                                if (mo<15) {
                                    current=60-(60-mo)-1;
                                }
                                if (mo<59&&mo>56){
                                    current=60L;
                                }

                                myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), current, 59, take.getBegin()));
                            }
                            myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), 500, 5, take.getBegin()));
                            continue;
                        }


                        log.info(System.currentTimeMillis()/1000+"   userCall "+userCall);

                        log.info("channel69: {}", channel);
                        if (take.getFlag() > 0) {
                            Interactive end = interactiveService.getById(channel.getBegin() + take.getChannelName() + "0000");
                            //初始化通道,踢出所有人
                            try {
                                agoraService.suspendChannel(channel.getChannel(), 0);
                                //更新通道状态为关闭状态
                                Boolean closed = rtcSessionCache.channelEnd(from, to, take.getFlag());
                                if (!closed) {
                                    rtcSessionCache.channelEnd(from, to, take.getFlag());
                                }
                            } catch (Exception e) {
                                log.error(" " + e.fillInStackTrace());
                            }
                            Interactive interactive = new Interactive();
                            interactive.setId(channel.getBegin() + take.getChannelName() + "0000");
                            interactive.setUserCode(from);
                            interactive.setCreator(to);
                            interactive.setToken(TokenEnum.ETH.getSymbol());

                            interactive.setCreateTime(new Date(currentTimeMillis).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                            interactive.setUpdateTime(new Date(currentTimeMillis).toInstant().atOffset(ZoneOffset.of("+8")).toLocalDateTime());
                            interactive.setStatus(InteractiveStautsEnum.CALL_END.getType());
                            interactive.setAmount("");
                            interactive.setTimeMillis(System.currentTimeMillis());
                            //钱包服务器结算成功
                            if (end == null) {
                                interactiveService.save(interactive);
                                Interactive updateBegin = new Interactive();
                                updateBegin.setId(channel.getBegin() + take.getChannelName());
                                updateBegin.setReserved("0000");
                                updateBegin.setUpdateTime(LocalDateTime.now());

                                interactiveService.updateById(updateBegin);
                            }


                        } else {//获取to当前单价
                            Boolean hasEnd = false;
                            Iterator<CallDelayTask> iterator = myCallDelayQueue.iterator();
                            //查询当前队列是否有挂断队列
                            while (iterator.hasNext()) {
                                CallDelayTask next = iterator.next();
                                //相同的ChannelName 并且Flag不为0 有中断操作,当前扣费操作取消
                                if (take.getChannelName().equals(next.getChannelName()) && next.getFlag() != null && (next.getFlag() > 0 &&next.getFlag()!=99)) {
                                    hasEnd = true;
                                    break;
                                }
                            }
                            //不做操作 直接取出下个队列
                            if (hasEnd) {
                                continue;
                            }

                            if (userCall==null||size==0||(System.currentTimeMillis() / 1000 - (userCall.getJSONObject(size - 1).getLong("time")) > 15))//限定时间内未扣费
                            {
                                log.info("userCall end  "+System.currentTimeMillis() / 1000+"  "+(userCall.getJSONObject(size - 1).getLong("time")) );
                                myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), 500, 3, take.getBegin()));
                                continue;
                            }
                            //预扣费
                            long d = currentTimeMillis  - take.getBegin()/1000;
                            long mo = d  % 60;
                            //扣费成功
                            rtcSessionCache.channelFrozen(from, to, "");
                            Long current = (60 - mo) + 10;
                            log.info("current161 :{},{},{}", current, (currentTimeMillis - take.getBegin()) / 1000, ((currentTimeMillis - take.getBegin()) / 1000) % 60);
                            //下次扣费
                            myCallDelayQueue.add(new CallDelayTask(from, to, channel.getChannel(), current * 1000, 0, take.getBegin()));
                        }

                    } catch (Exception e) {
                        log.error(e.fillInStackTrace() + "");
                    }
                }
            } catch (InterruptedException e) {
                // 因为是重写Runnable接口的run方法，子类抛出的异常要小于等于父类的异常。而在Runnable中run方法是没有抛异常的。
                // 所以此时是不能抛出InterruptedException异常。如果此时你只是记录日志的话，那么就是一个不负责任的做法，
                // 因为在捕获InterruptedException异常的时候自动的将是否请求中断标志置为了false。
                // 在捕获了InterruptedException异常之后，如果你什么也不想做，那么就将标志重新置为true，以便栈中更高层的代码能知道中断，并且对中断作出响应。
                Thread.currentThread().interrupt();
            }
        }). start();

    }



    @SneakyThrows
    public static void main1() {
        long temp1 = 50;
        long min = temp1 / 60;
        long mo = temp1 % 60;
        System.out.println("" + temp1 + " " + min + " " + mo);

        long l = System.currentTimeMillis();
        long current = 59;
        new Thread(() -> {
            while (true) {
                System.out.println("            " + (System.currentTimeMillis() - l) / 1000);
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        TimeUnit.SECONDS.sleep(current);
        while (true) {
            current = (System.currentTimeMillis() - l) / 1000;
            System.out.println(current);
            long temp = current % 60;
            current = (60 - temp) + 2;


            System.out.println(current + "  " + ((System.currentTimeMillis() - l) / 1000) + " " + ((System.currentTimeMillis() - l) / 1000) % 60);
            TimeUnit.SECONDS.sleep(current);
        }

    }
}

