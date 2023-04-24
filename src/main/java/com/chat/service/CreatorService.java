package com.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.common.ChatPage;
import com.chat.entity.Creator;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.entity.dto.CreatorDto;
import com.chat.entity.dto.CreatorSearchDto;
import com.github.yulichang.base.MPJBaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
public interface CreatorService extends MPJBaseService<Creator> {

    ChatPage<CreatorDto> popular(Page<CreatorDto> page, CreatorSearchDto query);
}
