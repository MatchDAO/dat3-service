package com.chat.mapper;

import com.chat.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
