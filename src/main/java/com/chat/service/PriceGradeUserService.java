package com.chat.service;

import com.chat.entity.PriceGradeUser;
import com.chat.entity.dto.PriceGradeUserDto;
import com.github.yulichang.base.MPJBaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
public interface PriceGradeUserService extends MPJBaseService<PriceGradeUser> {

    PriceGradeUserDto grade(String userCode) throws Exception;

    Boolean modifyGrade(String userCode, Integer id);
}
