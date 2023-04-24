package com.chat.entityMapper;

import com.chat.entity.Creator;
import com.chat.entity.dto.CreatorDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface Creator2DtoMapper {
    Creator2DtoMapper INSTANCE = Mappers.getMapper(Creator2DtoMapper.class);

    //todo 时区问题
//    @Mapping(target = "online",expression="java((creator.getOnline()!=null&&1==creator.getOnline())?System.currentTimeMillis():0)")
//    CreatorDto Creator2Dto(Creator creator);


    @Mapping(target = "online", qualifiedByName = "defaultOnline")
    CreatorDto Creator2Dto(Creator creator);

    List<CreatorDto> CreatorList2Dto(List<Creator> creators);

    //todo 时区问题
    @Named("defaultOnline")
    default Long defaultOnline(Integer online) {
        if (online != null && 1 == online) {
            return System.currentTimeMillis();
        }
        return 0L;
    }
}
