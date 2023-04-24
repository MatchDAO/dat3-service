package com.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.common.R;
import com.chat.common.TokenEnum;
import com.chat.config.ChatConfig;
import com.chat.entity.PriceGradeUser;
import com.chat.entity.PriceRange;
import com.chat.entity.dto.PriceGradeUserDto;
import com.chat.entity.dto.WalletTicker;
import com.chat.mapper.PriceGradeUserMapper;
import com.chat.service.PriceGradeUserService;
import com.chat.service.TransactionUtils;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Service
@Slf4j
public class PriceGradeUserServiceImpl extends MPJBaseServiceImpl<PriceGradeUserMapper, PriceGradeUser> implements PriceGradeUserService {

    @Resource
    private PriceRangeServiceImpl priceRangeService;

    @Override
    public PriceGradeUserDto grade(String userCode) throws Exception {
        MPJLambdaWrapper<PriceGradeUser> wrapper = new MPJLambdaWrapper<PriceGradeUser>()
                .select(PriceGradeUser::getUserCode, PriceGradeUser::getRangeId)
                .selectAll(PriceRange.class)
                .leftJoin(PriceRange.class, PriceRange::getId, PriceGradeUser::getRangeId)
                .eq(PriceGradeUser::getUserCode, userCode);
        wrapper.last("limit 1");
        PriceGradeUserDto priceGradeUserDtos = this.selectJoinOne(PriceGradeUserDto.class, wrapper);

        if (priceGradeUserDtos == null) {
            PriceGradeUser one = new PriceGradeUser();
            one.setUserCode(userCode);
            one.setRangeId(1);
            this.save(one);
            priceGradeUserDtos = this.selectJoinOne(PriceGradeUserDto.class, wrapper);
        }
        String price = priceGradeUserDtos.getPrice();
        BigDecimal multiply = BigDecimal.ZERO;
        ;
        if (!StrUtil.isEmpty(price)) {
            multiply = new BigDecimal(price).multiply(BigDecimal.TEN.pow(8));
        } else {
            PriceRange byId = priceRangeService.getById(1);
            multiply = new BigDecimal(byId.getPrice()).multiply(BigDecimal.TEN.pow(8));
        }
        priceGradeUserDtos.setEPrice(multiply.toBigInteger().toString());
        log.info("" + priceGradeUserDtos);
        return priceGradeUserDtos;
    }


    @Override
    public Boolean modifyGrade(String userCode, Integer id) {
        List<PriceGradeUser> list = this.list(new LambdaQueryWrapper<PriceGradeUser>().eq(PriceGradeUser::getUserCode, userCode));

        if (CollUtil.isEmpty(list)) {
            PriceGradeUser gradeUser = new PriceGradeUser();
            gradeUser.setUserCode(userCode);
            gradeUser.setRangeId(id);
            return this.save(gradeUser);
        }
        if (list.size() > 1) {
            this.remove(new LambdaQueryWrapper<PriceGradeUser>().eq(PriceGradeUser::getUserCode, userCode));
            PriceGradeUser gradeUser = new PriceGradeUser();
            gradeUser.setUserCode(userCode);
            gradeUser.setRangeId(id);
            return this.save(gradeUser);
        }
        PriceGradeUser one = list.get(0);
        if (!id.equals(one.getRangeId())) {
            one.setRangeId(id);
            return this.updateById(one);
        }
        return false;

    }

}
