package com.chat.entity.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchDto {


   // @ApiModelProperty(value = "会员账户,手机号或邮箱",required = true)
    private String email;

   // @ApiModelProperty("会员昵称查询")
    private String username;
   // @ApiModelProperty("国家的手机编码")
    private String wallet;

   // @ApiModelProperty("手机号查询")
    private String tag;

    //@ApiModelProperty("时间区间查询的开始时间")
    private  Long beginTime;
   // @ApiModelProperty("时间区间查询的结束时间")
    private  Long endTime;

    private Long current;
    private Long size;
    private String address;
    private Integer gender;
}
