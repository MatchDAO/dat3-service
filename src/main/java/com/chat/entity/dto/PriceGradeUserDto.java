package com.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceGradeUserDto {

    private Integer id;


    private Integer rangeId;
    private String price;
    private String ePrice;
    private String userCode;

    @Override
    public String toString() {
        return "PriceGradeUserDto{" +
                "id=" + id +
                ", rangeId=" + rangeId +
                ", price=" + price +
                ", ePrice='" + ePrice + '\'' +
                ", userCode='" + userCode + '\'' +
                '}';
    }
}
