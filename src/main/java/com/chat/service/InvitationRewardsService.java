package com.chat.service;

import com.chat.entity.InvitationRewards;
import com.chat.entity.dto.InvitationRewardsDto;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-12-21
 */
public interface InvitationRewardsService extends MPJBaseService<InvitationRewards> {

    List invitationCodeList(String userCode);
    Boolean setInvitationRewards(String type ,String userCode, String invitationCode);
    boolean updateInvitationRewards(String type,String amount,String invited, String invitationCode);

    List<InvitationRewardsDto> rewardsList(String userCode);

}
