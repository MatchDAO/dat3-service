package com.chat.cache;


import cn.hutool.core.util.RandomUtil;
import com.chat.entity.dto.AptosRequestTask;
import com.chat.service.AptosService;
import com.chat.utils.aptos.AptosClient;
import com.chat.utils.aptos.request.v1.model.Response;
import com.chat.utils.aptos.request.v1.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RunnerInit {
    @Resource
    private AptosService aptosService;
    @Resource
    private AptosClient aptosClient;
    private ScheduledThreadPoolExecutor mPoolExecutor = new ScheduledThreadPoolExecutor(15);

    @PostConstruct
    public void CacheInit() {

        mPoolExecutor.execute(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    AptosRequestTask take = aptosService.task.take();
                    try {
                        if (take.getPayload()!=null  ) {
                            if (take.getSimulate()) {
                                Response<Transaction> transactionResponse1 = aptosClient.requestSimulateTransaction(take.getSig(), take.getPayload());
                                String gasUsed = transactionResponse1.getData().getGasUsed();
                                log.info(gasUsed);
                                List arguments = take.getPayload().getArguments();
                                int size = arguments.size();
                                arguments.set(size-1,new BigDecimal(gasUsed).multiply(new BigDecimal(transactionResponse1.getData().getGasUnitPrice())).toBigInteger().toString());
                                take.getPayload().setArguments(arguments);
                            }
                            Response<Transaction> transactionResponse = aptosClient.requestSubmitTransaction(take.getSig(), take.getPayload());
                        }
                    }catch (Exception e){
                       log.info(""+e.fillInStackTrace());
                    }

                    System.out.println(take);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });
    }
}
