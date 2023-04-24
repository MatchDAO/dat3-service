package com.chat.service;

import com.chat.common.R;
import com.chat.entity.Interactive;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.entity.dto.InteractiveDto;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
public interface InteractiveService extends IService<Interactive> {

    R sendMsg(InteractiveDto interactiveDto);

    R reply(String creator, String userCode ,Long timestamp);
    boolean updateInteractiveStauts(String stauts, List<String> interactiveId);

    R before(String u1,String u2);

    R sendMsgv1(InteractiveDto interactiveDto);

}
