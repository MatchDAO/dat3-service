package com.chat.entityMapper;

import com.chat.entity.PriceRange;
import com.chat.entity.dto.PriceRangeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface PriceRange2DtoMapper {
    PriceRange2DtoMapper INSTANCE = Mappers.getMapper(PriceRange2DtoMapper.class);

//    @Mapping(target = "ePrice", source = "price")
    PriceRangeDto PriceRange2Dto(PriceRange priceRange );

    List<PriceRangeDto> PriceRangeList2Dto(List<PriceRange> priceRanges);
}
