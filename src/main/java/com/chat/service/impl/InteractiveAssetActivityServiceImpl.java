package com.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.common.TokenEnum;
import com.chat.entity.InteractiveAssetActivity;
import com.chat.mapper.InteractiveAssetActivityMapper;
import com.chat.service.InteractiveAssetActivityService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Service
public class InteractiveAssetActivityServiceImpl
        extends ServiceImpl<InteractiveAssetActivityMapper, InteractiveAssetActivity>
        implements InteractiveAssetActivityService {

    @Override
    public Boolean addAssetActivity(String creator, String consumer, String token, String amount, int sentCount, String transactionOrderId) {
        save(InteractiveAssetActivity.builder()
                .id(transactionOrderId)
                .fromAddress(consumer)
                .toAddress(creator)
                .token(token)
                .amount(amount)
                .sentCount(sentCount)
                .fromProfitToken(TokenEnum.DAT3APTOS.getSymbol())
                .fromProfitAmount("0")
                .fromSpendAmount(amount)
                .fromSpendToken(token)
                .toProfitToken(TokenEnum.DAT3APTOS.getSymbol())
                .toProfitAmount("0")
                .createTime(LocalDateTime.now())
                .build());
        return null;
    }
}
