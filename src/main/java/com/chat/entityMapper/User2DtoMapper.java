package com.chat.entityMapper;

import com.chat.entity.User;
import com.chat.entity.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface User2DtoMapper {
    User2DtoMapper INSTANCE = Mappers.getMapper(User2DtoMapper.class);
    UserDto user2Dto(User user);
    List<UserDto> user2vo(List<User> user);

}
