package com.chat.service;

import com.chat.entity.InvitationCodeTotal;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
public interface InvitationCodeTotalService extends IService<InvitationCodeTotal> {

    Integer changeTotal(String action, Integer change,String userCode,int used);

}
