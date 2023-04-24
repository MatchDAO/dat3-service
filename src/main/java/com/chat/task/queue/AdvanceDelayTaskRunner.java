package com.chat.task.queue;

import com.chat.cache.RtcSessionCache;
import com.chat.common.InteractiveStautsEnum;
import com.chat.common.TokenEnum;
import com.chat.config.ChatConfig;
import com.chat.config.own.PrivateConfig;
import com.chat.entity.Interactive;
import com.chat.entity.User;
import com.chat.entity.dto.ChannelDto;
import com.chat.entity.dto.PriceGradeUserDto;
import com.chat.entity.dto.WalletUserResult;
import com.chat.service.AgoraService;
import com.chat.service.TransactionUtils;
import com.chat.service.impl.InteractiveServiceImpl;
import com.chat.service.impl.PriceGradeUserServiceImpl;
import com.chat.service.impl.UserServiceImpl;
import com.chat.utils.Coder;
import com.chat.utils.DESCoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AdvanceDelayTaskRunner implements InitializingBean {
    @Resource
    private DelayQueue<AdvanceDelayTask> myDelayQueue;
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

    //结束原因 1:from挂断 2:to挂断 3:from资金不足 4未知原因(from/to未知原因中断),5to拒绝
    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                log.info("MsgDelayTaskRunner {}", myDelayQueue.size());
                while (true) {
                    AdvanceDelayTask take = myDelayQueue.take();
                    log.info("take:{}", take);
                    log.info("take: {},{},{},{}", take.getChannelName(), take.getDelay(TimeUnit.SECONDS), take.getFrom(), take.getTo());
                    try {
                        //获取用户信息
                        String from = take.getFrom();
                        String to = take.getTo();
                        User fromUser = userService.getById(from);
                        User toUser = userService.getById(to);
                        if (fromUser == null || toUser == null) {
                            log.error("AdvanceDelayTaskRunner :no user info");
                            continue;
                        }
                        ChannelDto channel = rtcSessionCache.channel(from, to);
                        log.info("channel69: {}", channel);
                        if (take.getFlag() > 0) {
                            //获取互动开始时间
                            Interactive end = interactiveService.getById(take.getBegin() + take.getChannelName() + "0000");
                            log.info("begin,end: {},----{}", take.getBegin(), end);

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
                            WalletUserResult walletUserResult = null;
                            //预扣费结算
                            try {
                                String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(fromUser.getWallet().getBytes(), PrivateConfig.TR_KEY))
                                        .replace("\n", ""));
                                walletUserResult = transactionUtils.rtcPayment(fromUser.getWallet(), toUser.getWallet(), password, take.getBegin() + "", take.getChannelName());

                            } catch (Exception e) {
                                log.error("96 " + e.fillInStackTrace());
                            }
                            Interactive interactive = new Interactive();
                            interactive.setId(take.getBegin() + take.getChannelName() + "0000");
                            interactive.setUserCode(from);
                            interactive.setCreator(to);
                            interactive.setToken(TokenEnum.ETH.getSymbol());

                            interactive.setCreateTime(LocalDateTime.now());
                            interactive.setUpdateTime(LocalDateTime.now());
                            interactive.setStatus(InteractiveStautsEnum.CALL_END.getType());
                            if (walletUserResult != null && walletUserResult.getCode() == 200) {
                                interactive.setAmount(walletUserResult.getMessage() == null ? "0" : walletUserResult.getMessage());
                                //钱包服务器结算成功
                                if (end == null) {
                                    interactiveService.save(interactive);
                                }
                                Interactive updateBegin = new Interactive();
                                updateBegin.setId(take.getBegin() + take.getChannelName());
                                updateBegin.setReserved("0000");
                                updateBegin.setUpdateTime(LocalDateTime.now());
                                interactiveService.updateById(updateBegin);
                            } else if (walletUserResult != null && walletUserResult.getCode() == 1001) {
                                //钱包服务器已结算过
                                interactive.setAmount(walletUserResult.getMessage() == null ? "0" : walletUserResult.getMessage());
                                //记录开始和结束
                                if (end == null) {
                                    interactiveService.save(interactive);
                                }
                                Interactive updateBegin = new Interactive();
                                updateBegin.setId(take.getBegin() + take.getChannelName());
                                updateBegin.setReserved("0000");
                                updateBegin.setUpdateTime(LocalDateTime.now());
                                interactiveService.updateById(updateBegin);
                            }
                            else {
                                log.error("take.getFlag() > 0  "+walletUserResult);
                                Interactive updateBegin = new Interactive();
                                updateBegin.setId(take.getBegin() + take.getChannelName());
                                updateBegin.setReserved("0000");
                                updateBegin.setUpdateTime(LocalDateTime.now());
                                interactiveService.updateById(updateBegin);
                            }
                            continue;
                        } else {//获取to当前单价

                            //获取当前通道
                            Long end = channel.getEnd();
                            //异常中断 需判断是否强制中断
                            if (end != null && end > 0) {
                                Integer reason = channel.getReason();
                                reason = (reason != null && reason > 0) ? reason : 4;
                                myDelayQueue.add(new AdvanceDelayTask(from, to, channel.getChannel(), 500, reason, take.getBegin()));
                                continue;
                            }
                            Boolean hasEnd = false;
                            Iterator<AdvanceDelayTask> iterator = myDelayQueue.iterator();
                            //查询当前队列是否有挂断队列
                            while (iterator.hasNext()) {
                                AdvanceDelayTask next = iterator.next();
                                //相同的ChannelName 并且Flag不为0 有中断操作,当前扣费操作取消
                                if (take.getChannelName().equals(next.getChannelName())
                                        &&next.getFlag() != null
                                        &&next.getFlag() > 0) {
                                    hasEnd = true;
                                    break ;
                                }
                            }
                            //不做操作 直接取出下个队列
                            if (hasEnd){
                                continue;
                            }
                            PriceGradeUserDto grade = priceGradeUserService.grade(to);
                            log.info("grade 158: {}", grade);
                            WalletUserResult walletUserResult = null;
                            try {
                                String password = URLEncoder.encode(Coder.encryptBASE64(DESCoder.encrypt(fromUser.getWallet().getBytes(), PrivateConfig.TR_KEY))
                                        .replace("\n", ""));
                                walletUserResult = transactionUtils.rtcFrozen(fromUser.getWallet(), toUser.getWallet(), grade.getEPrice(), password, System.currentTimeMillis() + "");

                            } catch (Exception e) {
                                log.error("146 " + e.fillInStackTrace());
                            }
                            //预扣费
                            log.info(" 168 walletUserResult {}", walletUserResult);
                            if (walletUserResult != null && walletUserResult.getCode() == 200) {
                                long currentTimeMillis = System.currentTimeMillis();
                                //扣费成功
                                rtcSessionCache.channelFrozen(from, to, grade.getEPrice());
                                Long current = (currentTimeMillis - channel.getBegin()) / 1000;
                                // 60之前 ->55;56;557;52;
                                // current = current % 60 < 50 ? (55 - (current % 60)) + 60 : 60;
                                // 60之后 61;63;65
                                long temp=current % 60;
                                current= (60 - temp)+2;

                                log.info("current161 :{},{},{}", current, (currentTimeMillis - take.getBegin()) / 1000, ((currentTimeMillis - take.getBegin()) / 1000) % 60);

                                //下次扣费
                                myDelayQueue.add(new AdvanceDelayTask(from, to, channel.getChannel(), current * 1000, 0, take.getBegin()));
                                continue;
                            }
                            //扣费不成功 需判断是否强制中断
                            myDelayQueue.add(new AdvanceDelayTask(from, to, channel.getChannel(), 500, 3, take.getBegin()));
                        }

                    } catch (Exception e) {
                        log.error(e.fillInStackTrace() + "");
                    }
                }
            } catch (InterruptedException e) {
                // 因为是重写Runnable接口的run方法，子类抛出的异常要小于等于父类的异常。而在Runnable中run方法是没有抛异常的。所以此时是不能抛出InterruptedException异常。如果此时你只是记录日志的话，那么就是一个不负责任的做法，因为在捕获InterruptedException异常的时候自动的将是否请求中断标志置为了false。在捕获了InterruptedException异常之后，如果你什么也不想做，那么就将标志重新置为true，以便栈中更高层的代码能知道中断，并且对中断作出响应。
                Thread.currentThread().interrupt();
            }
        }).start();

    }

    @SneakyThrows
    public static void main(String[] args) {
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
            long temp=current % 60;
            current= (60 - temp)+2;


            System.out.println(current + "  " + ((System.currentTimeMillis() - l) / 1000) + " " + ((System.currentTimeMillis() - l) / 1000) % 60);
            TimeUnit.SECONDS.sleep(current);
        }

    }
}

