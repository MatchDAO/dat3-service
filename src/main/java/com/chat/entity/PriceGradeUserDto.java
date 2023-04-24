package com.chat.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceGradeUserDto {

    private Integer id;


    private Integer rangeId;
    private Integer price;
    private String ePrice;
    private String userCode;
}
