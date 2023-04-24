package com.chat.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 互动表
 * </p>
 *
 * @author jetBrains
 * @since 2022-11-17
 */
@Getter
@Setter
public class InteractiveDto implements Serializable {

    private static final long serialVersionUID = 1L;


    private String userCode;

    private String professionType;

    private String creator;

    private String amount;

    private String token;

    private Long timestamp;


}
