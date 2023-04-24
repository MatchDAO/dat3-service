package com.chat.service;

import com.chat.common.TokenEnum;
import com.chat.entity.InteractiveAssetActivity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
public interface InteractiveAssetActivityService extends IService<InteractiveAssetActivity> {

    Boolean addAssetActivity(String creator ,String  consumer ,   String token  , String amount ,int replyCount ,String transactionOrderId);

}
