package com.chat.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chat.cache.ChatFeeCache;
import com.chat.common.R;
import com.chat.common.TokenEnum;
import com.chat.config.ChatConfig;
import com.chat.entity.PriceRange;
import com.chat.entity.dto.PriceRangeDto;
import com.chat.entity.dto.WalletTicker;
import com.chat.entityMapper.PriceRange2DtoMapper;
import com.chat.service.MetadataService;
import com.chat.service.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class MetadataServiceImpl implements MetadataService {

    @Resource
    private TransactionUtils transactionUtils;
    @Resource
    private PriceRangeServiceImpl priceRangeService;

    @Override
    public R chatFee() {
        try {
            WalletTicker ticker = transactionUtils.exchangePrice(TokenEnum.APT.getSymbol(), TokenEnum.USDTERC.getSymbol());
            String price = ticker.getPrice();
            //  e/u=1000    u=e/1000
            JSONObject entries = JSONUtil.parseObj(ticker);
            entries.put("chatFee", ChatConfig.CHAT_FEE);
            BigDecimal multiply = BigDecimal.ZERO;
            if (!"0".equals(price)) {
                multiply = BigDecimal.ONE.divide(new BigDecimal(price), 12, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(ChatConfig.CHAT_FEE)).multiply(BigDecimal.TEN.pow(8));
            }
            entries.put("ethChatFee", multiply.toBigInteger().toString());
            ChatFeeCache.chatFee = multiply.toBigInteger();
            return R.success(entries);
        } catch (Exception e) {
            log.error("chatFee:::::" + e);
            return R.error();
        }
    }
    public R grade() throws Exception {
        List<PriceRange> list = priceRangeService.list();


        List<PriceRangeDto> dtos = PriceRange2DtoMapper.INSTANCE.PriceRangeList2Dto(list );
        dtos.forEach(s->{
            s.setePrice(BigDecimal.TEN.pow(8).multiply(new BigDecimal(s.getPrice())).toBigInteger().toString());
        });
        return R.success(dtos);
    }

}
