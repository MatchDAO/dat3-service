package com.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.common.R;
import com.chat.entity.User;
import com.chat.entity.dto.UserAuthDto;
import com.chat.entity.dto.UserDto;
import com.chat.entity.dto.UserWalletAuth;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jetBrains
 * @since 2022-10-20
 */
public interface IUserService extends IService<User> {

    R register(UserAuthDto newUser, String login) throws Exception;

    R registerV1(UserWalletAuth newUser);
    R registerEnd(UserDto query, MultipartFile file, String ipv4, String region);

    R login(UserAuthDto newUser) throws Exception;

    UserDto getUserInfo(UserDto userDto,Boolean re);
    R getUser(UserDto userDto);

    R modifyUserInfo(UserDto query, MultipartFile file);

    R modifyUserPortrait(String userCode, MultipartFile file);
    Boolean modifyUserInfoBase(String userCode,String name, String file);

    R logout(String email);

    R registerV1End(UserDto newUser, MultipartFile file, String ipv4, String region);

    boolean checkUserName(String userName);
}
