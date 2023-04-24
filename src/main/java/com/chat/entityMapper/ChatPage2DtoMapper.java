package com.chat.entityMapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chat.common.ChatPage;
import com.chat.entity.User;
import com.chat.entity.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ChatPage2DtoMapper {
    ChatPage2DtoMapper INSTANCE = Mappers.getMapper(ChatPage2DtoMapper.class);
    ChatPage IPage2ChatPage(IPage user);

}
